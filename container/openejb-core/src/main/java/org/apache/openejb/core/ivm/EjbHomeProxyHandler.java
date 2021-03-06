/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.core.ivm;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.ejb.AccessLocalException;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;

import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.InterfaceType;
import org.apache.openejb.ProxyInfo;
import org.apache.openejb.core.ServerFederation;
import org.apache.openejb.core.entity.EntityEjbHomeHandler;
import org.apache.openejb.core.stateless.StatelessEjbHomeHandler;
import org.apache.openejb.core.stateful.StatefulEjbHomeHandler;
import org.apache.openejb.spi.ApplicationServer;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;
import org.apache.openejb.util.proxy.ProxyManager;

public abstract class EjbHomeProxyHandler extends BaseEjbProxyHandler {
    private static final Logger logger = Logger.getInstance(LogCategory.OPENEJB, "org.apache.openejb.util.resources");

    private final Map<String, MethodType> dispatchTable;

    private static enum MethodType {
        CREATE,
        FIND,
        HOME_HANDLE,
        META_DATA,
        REMOVE
    }

    public EjbHomeProxyHandler(DeploymentInfo deploymentInfo, InterfaceType interfaceType, List<Class> interfaces) {
        super(deploymentInfo, null, interfaceType, interfaces);
        dispatchTable = new HashMap<String, MethodType>();
        dispatchTable.put("create", MethodType.CREATE);
        dispatchTable.put("getEJBMetaData", MethodType.META_DATA);
        dispatchTable.put("getHomeHandle", MethodType.HOME_HANDLE);
        dispatchTable.put("remove", MethodType.REMOVE);

        if (interfaceType.isHome()) {
            Class homeInterface = deploymentInfo.getInterface(interfaceType);
            Method[] methods = homeInterface.getMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("create")) {
                    dispatchTable.put(method.getName(), MethodType.CREATE);
                } else if (method.getName().startsWith("find")) {
                    dispatchTable.put(method.getName(), MethodType.FIND);
                }
            }
        }

    }

    public void invalidateReference() {
        throw new IllegalStateException("A home reference must never be invalidated!");
    }

    protected static EjbHomeProxyHandler createHomeHandler(DeploymentInfo deploymentInfo, InterfaceType interfaceType, List<Class> interfaces) {
        switch (deploymentInfo.getComponentType()) {
            case STATEFUL:
                return new StatefulEjbHomeHandler(deploymentInfo, interfaceType, interfaces);
            case STATELESS:
                return  new StatelessEjbHomeHandler(deploymentInfo, interfaceType, interfaces);
            case CMP_ENTITY:
            case BMP_ENTITY:
                return  new EntityEjbHomeHandler(deploymentInfo, interfaceType, interfaces);
            default: throw new IllegalStateException("Component type does not support rpc interfaces: " + deploymentInfo.getComponentType());
        }
    }

    public static Object createHomeProxy(DeploymentInfo deploymentInfo, InterfaceType interfaceType) {
        return createHomeProxy(deploymentInfo, interfaceType, null);
    }

    public static Object createHomeProxy(DeploymentInfo deploymentInfo, InterfaceType interfaceType, List<Class> objectInterfaces) {
        if (!interfaceType.isHome()) throw new IllegalArgumentException("InterfaceType is not a Home type: " + interfaceType);

        try {
            EjbHomeProxyHandler handler = createHomeHandler(deploymentInfo, interfaceType, objectInterfaces);

            List<Class> proxyInterfaces = new ArrayList<Class>(2);

            Class homeInterface = deploymentInfo.getInterface(interfaceType);
            proxyInterfaces.add(homeInterface);
            proxyInterfaces.add(IntraVmProxy.class);

            return ProxyManager.newProxyInstance(proxyInterfaces.toArray(new Class[]{}), handler);
        } catch (Exception e) {
            throw new RuntimeException("Can't create EJBHome stub" + e.getMessage(), e);
        }
    }

    public Object createProxy(Object primaryKey) {
        try {

            InterfaceType objectInterfaceType = this.interfaceType.getCounterpart();

            EjbObjectProxyHandler handler = newEjbObjectHandler(getDeploymentInfo(), primaryKey, objectInterfaceType, this.getInterfaces());

            List<Class> proxyInterfaces = new ArrayList<Class>(handler.getInterfaces().size() + 1);

            proxyInterfaces.addAll(handler.getInterfaces());
            proxyInterfaces.add(IntraVmProxy.class);

            return ProxyManager.newProxyInstance(proxyInterfaces.toArray(new Class[]{}), handler);

        } catch (IllegalAccessException iae) {
            throw new RuntimeException("Could not create IVM proxy for " + getInterfaces().get(0), iae);
        }
    }

    protected abstract EjbObjectProxyHandler newEjbObjectHandler(DeploymentInfo deploymentInfo, Object pk, InterfaceType interfaceType, List<Class> interfaces);

    protected Object _invoke(Object proxy, Class interfce, Method method, Object[] args) throws Throwable {

        if (logger.isInfoEnabled()) {
            logger.info("invoking method " + method.getName() + " on " + deploymentID);
        }

        String methodName = method.getName();

        try {
            java.lang.Object retValue;
            MethodType operation = dispatchTable.get(methodName);

            if (operation == null) {
                retValue = homeMethod(interfce, method, args, proxy);
            } else {
                switch (operation) {
                    /*-- CREATE ------------- <HomeInterface>.create(<x>) ---*/
                    case CREATE:
                        retValue = create(interfce, method, args, proxy);
                        break;
                    case FIND:
                        retValue = findX(interfce, method, args, proxy);
                        break;
                        /*-- GET EJB METADATA ------ EJBHome.getEJBMetaData() ---*/
                    case META_DATA:
                        retValue = getEJBMetaData(method, args, proxy);
                        break;
                        /*-- GET HOME HANDLE -------- EJBHome.getHomeHandle() ---*/
                    case HOME_HANDLE:
                        retValue = getHomeHandle(method, args, proxy);
                        break;
                        /*-- REMOVE ------------------------ EJBHome.remove() ---*/
                    case REMOVE: {
                        Class type = method.getParameterTypes()[0];

                        /*-- HANDLE ------- EJBHome.remove(Handle handle) ---*/
                        if (javax.ejb.Handle.class.isAssignableFrom(type)) {
                            retValue = removeWithHandle(interfce, method, args, proxy);
                        } else {
                            /*-- PRIMARY KEY ----- EJBHome.remove(Object key) ---*/
                            retValue = removeByPrimaryKey(interfce, method, args, proxy);
                        }
                        break;
                    }
                    default:
                        throw new RuntimeException("Inconsistent internal state: value " + operation + " for operation " + methodName);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("finished invoking method " + method.getName() + ". Return value:" + retValue);
            } else if (logger.isInfoEnabled()) {
                logger.info("finished invoking method " + method.getName());
            }

            return retValue;

            /*
            * The ire is thrown by the container system and propagated by
            * the server to the stub.
            */
        } catch (RemoteException re) {
            if (interfaceType.isLocal()) {
                throw new EJBException(re.getMessage(), (Exception) re.detail);
            } else {
                throw re;
            }

        } catch (org.apache.openejb.InvalidateReferenceException ire) {
            Throwable cause = ire.getRootCause();
            if (cause instanceof RemoteException && interfaceType.isLocal()) {
                RemoteException re = (RemoteException) cause;
                Throwable detail = (re.detail != null) ? re.detail : re;
                cause = new EJBException(re.getMessage(), (Exception) detail);
            }
            throw cause;
            /*
            * Application exceptions must be reported dirctly to the client. They
            * do not impact the viability of the proxy.
            */
        } catch (org.apache.openejb.ApplicationException ae) {
            Throwable exc = (ae.getRootCause() != null) ? ae.getRootCause() : ae;
            if (exc instanceof EJBAccessException) {
                if (interfaceType.isBusiness()) {
                    throw exc;
                } else {
                    if (interfaceType.isLocal()) {
                        throw (AccessLocalException)new AccessLocalException(exc.getMessage()).initCause(exc);
                    } else {
                        throw (AccessException)new AccessException(exc.getMessage()).initCause(exc);
                    }
                }

            }
            throw exc;
            /*
            * A system exception would be highly unusual and would indicate a sever
            * problem with the container system.
            */
        } catch (org.apache.openejb.SystemException se) {
            if (interfaceType.isLocal()) {
                throw new EJBException("Container has suffered a SystemException", (Exception) se.getRootCause());
            } else {
                throw new RemoteException("Container has suffered a SystemException", se.getRootCause());
            }
        } catch (org.apache.openejb.OpenEJBException oe) {
            if (interfaceType.isLocal()) {
                throw new EJBException("Unknown Container Exception", (Exception) oe.getRootCause());
            } else {
                throw new RemoteException("Unknown Container Exception", oe.getRootCause());
            }
        } catch (Throwable t) {
            logger.info("finished invoking method " + method.getName() + " with exception:" + t, t);
            throw t;
        }
    }

    /*-------------------------------------------------*/
    /*  Home interface methods                         */
    /*-------------------------------------------------*/

    protected Object homeMethod(Class interfce, Method method, Object[] args, Object proxy) throws Throwable {
        checkAuthorization(method);
        return container.invoke(deploymentID, interfce, method, args, null);
    }

    protected Object create(Class interfce, Method method, Object[] args, Object proxy) throws Throwable {
        ProxyInfo proxyInfo = (ProxyInfo) container.invoke(deploymentID, interfce, method, args, null);
        assert proxyInfo != null : "Container returned a null ProxyInfo: ContainerID=" + container.getContainerID();
        return createProxy(proxyInfo.getPrimaryKey());
    }

    protected abstract Object findX(Class interfce, Method method, Object[] args, Object proxy) throws Throwable;

    /*-------------------------------------------------*/
    /*  EJBHome methods                                */
    /*-------------------------------------------------*/

    protected Object getEJBMetaData(Method method, Object[] args, Object proxy) throws Throwable {
        checkAuthorization(method);
        IntraVmMetaData metaData = new IntraVmMetaData(getDeploymentInfo().getHomeInterface(), getDeploymentInfo().getRemoteInterface(), getDeploymentInfo().getPrimaryKeyClass(), getDeploymentInfo().getComponentType());
        metaData.setEJBHome((EJBHome) proxy);
        return metaData;
    }

    protected Object getHomeHandle(Method method, Object[] args, Object proxy) throws Throwable {
        checkAuthorization(method);
        return new IntraVmHandle(proxy);
    }

    public org.apache.openejb.ProxyInfo getProxyInfo() {
        return new org.apache.openejb.ProxyInfo(getDeploymentInfo(), null, getDeploymentInfo().getInterfaces(interfaceType), interfaceType);
    }

    protected Object _writeReplace(Object proxy) throws ObjectStreamException {
        /*
         * If the proxy is being copied between bean instances in a RPC
         * call we use the IntraVmArtifact
         */
        if (IntraVmCopyMonitor.isIntraVmCopyOperation()) {
            return new IntraVmArtifact(proxy);
            /*
            * If the proxy is referenced by a stateful bean that is  being
            * passivated by the container we allow this object to be serialized.
            */
        } else if (IntraVmCopyMonitor.isStatefulPassivationOperation()) {
            return proxy;
            /*
            * If the proxy is being copied between class loaders
            * we allow this object to be serialized.
            */
        } else if (IntraVmCopyMonitor.isCrossClassLoaderOperation()) {
            return proxy;
            /*
            * If the proxy is serialized outside the core container system,
            * we allow the application server to handle it.
            */
        } else {
            ApplicationServer applicationServer = ServerFederation.getApplicationServer();
            return applicationServer.getEJBHome(this.getProxyInfo());
        }
    }

    protected Object removeWithHandle(Class interfce, Method method, Object[] args, Object proxy) throws Throwable {

        IntraVmHandle handle = (IntraVmHandle) args[0];
        Object primKey = handle.getPrimaryKey();
        EjbObjectProxyHandler stub;
        try {
            stub = (EjbObjectProxyHandler) ProxyManager.getInvocationHandler(handle.getEJBObject());
        } catch (IllegalArgumentException e) {

            stub = null;
        }

        container.invoke(deploymentID, interfce, method, args, primKey);

        /*
         * This operation takes care of invalidating all the EjbObjectProxyHanders associated with
         * the same RegistryId. See this.createProxy().
         */
        if (stub != null) {
            invalidateAllHandlers(stub.getRegistryId());
        }
        return null;
    }

    protected abstract Object removeByPrimaryKey(Class interfce, Method method, Object[] args, Object proxy) throws Throwable;
}
