package fyi.iapetus.plugins.acpolyfill.shared;

/*
    THIS CODE IS SHAMELESSLY STOLEN FROM ATLASSIAN
    Atlassian has provided a helper library to access OSGi exposed services
    that are not available to SAL ComponentLocator:

    com.atlassian.plugins:atlassian-plugins-osgi-javaconfig:0.6.0

    Unfortunately, due to the way this library is packaged we cannot use the helper library
    as a dependency, as Maven does not support transitive dependencies for libraries with classifiers (not sure why)

    For reasons unknown, Atlassian has removed access to sources of this helper library
    https://bitbucket.org/atlassian/atlassian-plugins-osgi-javaconfig/src/master/

    So this code has been reverse engineered.
    Technically it would have been doable to create similar code from scratch, but this was easier
    Thanks Atlassian!
 */

import java.time.Duration;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.eclipse.gemini.blueprint.service.importer.support.Availability;
import org.eclipse.gemini.blueprint.service.importer.support.OsgiServiceProxyFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public final class OsgiServices {

    public static <T> T importOsgiService(Class<T> serviceClass) {
        return invokeFactoryBean(factoryBeanForOsgiService(serviceClass));
    }

    private static <T> FactoryBean<T> factoryBeanForOsgiService(Class<T> serviceInterface) {
        OsgiServiceProxyFactoryBean factoryBean = new OsgiServiceProxyFactoryBean();
        factoryBean.setAvailability(Availability.MANDATORY);
        factoryBean.setBeanClassLoader(serviceInterface.getClassLoader());
        factoryBean.setInterfaces(new Class[] { serviceInterface });
        factoryBean.setTimeout(Duration.ofMinutes(5L).toMillis());
        return (FactoryBean<T>)factoryBean;
    }

    private static <T> T invokeFactoryBean(FactoryBean<T> factoryBean) {
        try {
            if (factoryBean instanceof BundleContextAware) {
                BundleContext bundleContext = FrameworkUtil.getBundle(OsgiServices.class).getBundleContext();
                ((BundleContextAware)factoryBean).setBundleContext(bundleContext);
            }
            if (factoryBean instanceof InitializingBean)
                ((InitializingBean)factoryBean).afterPropertiesSet();
            return (T)factoryBean.getObject();
        } catch (Exception e) {
            throw new BeanInitializationException(e.getMessage(), e);
        }
    }

}
