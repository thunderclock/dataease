package io.dataease.extensions.datasource.factory;

import io.dataease.exception.DEException;
import io.dataease.extensions.datasource.plugin.DataEaseDatasourcePlugin;
import io.dataease.extensions.datasource.provider.Provider;
import io.dataease.extensions.datasource.utils.SpringContextUtil;
import io.dataease.extensions.datasource.vo.DatasourceConfiguration;
import io.dataease.extensions.datasource.vo.XpackPluginsDatasourceVO;
import io.dataease.license.utils.LogUtil;
import io.dataease.plugins.factory.DataEasePluginFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Junjun
 */
public class ProviderFactory {

    public static Provider getProvider(String type) throws DEException {
        if (type.equalsIgnoreCase("es")) {
            return SpringContextUtil.getApplicationContext().getBean("esProvider", Provider.class);
        }
        List<String> list = Arrays.stream(DatasourceConfiguration.DatasourceType.values()).map(DatasourceConfiguration.DatasourceType::getType).toList();
        if (list.contains(type)) {
            return SpringContextUtil.getApplicationContext().getBean("calciteProvider", Provider.class);
        }
        Provider instance = getInstance(type);
        if (instance == null) {
            DEException.throwException("插件异常，请检查插件");
        }
        return instance;
    }

    public static Provider getDefaultProvider() {
        return SpringContextUtil.getApplicationContext().getBean("calciteProvider", Provider.class);
    }


    private static final Map<String, DataEaseDatasourcePlugin> templateMap = new ConcurrentHashMap<>();

    public static Provider getInstance(String type) {
        // 修改：移除企业版限制，允许社区版使用插件功能
        // if (!LicenseUtil.licenseValid()) DEException.throwException("插件功能只对企业版本可用！");
        String key = type;
        return templateMap.get(key);
    }

    public static void loadPlugin(String type, DataEaseDatasourcePlugin plugin) {
        // 修改：移除企业版限制，允许社区版使用插件功能
        // if (!LicenseUtil.licenseValid()) DEException.throwException("插件功能只对企业版本可用！");
        String key = type;
        if (templateMap.containsKey(key)) return;
        templateMap.put(key, plugin);
        try {
            String moduleName = plugin.getPluginInfo().getModuleName();
            DataEasePluginFactory.loadTemplate(moduleName, plugin);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), new Throwable(e));
            DEException.throwException(e);
        }
    }

    public static List<XpackPluginsDatasourceVO> getDsConfigList() {
        // 修改：移除企业版限制，允许社区版使用插件功能
        // if (!LicenseUtil.licenseValid()) DEException.throwException("插件功能只对企业版本可用！");
        return templateMap.values().stream().map(DataEaseDatasourcePlugin::getConfig).toList();
    }
}
