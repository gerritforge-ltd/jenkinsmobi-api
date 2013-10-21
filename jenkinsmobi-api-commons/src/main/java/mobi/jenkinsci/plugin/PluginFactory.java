package mobi.jenkinsci.plugin;

public interface PluginFactory {

  Plugin get(String pluginType);
}
