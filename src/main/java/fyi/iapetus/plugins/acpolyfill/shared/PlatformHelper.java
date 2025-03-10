package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.sal.api.ApplicationProperties;

public class PlatformHelper {

    public enum Platform {
        Jira,
        Confluence,
        Bitbucket,
        Bamboo,
        Unknown
    }

    private final ApplicationProperties applicationProperties;

    public PlatformHelper(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public boolean isJira() {
        return getPlatform().equals(Platform.Jira);
    }

    public boolean isConfluence() {
        return getPlatform().equals(Platform.Confluence);
    }

    public boolean isBamboo() {
        return getPlatform().equals(Platform.Bamboo);
    }

    public boolean isBitbucket() {
        return getPlatform().equals(Platform.Bitbucket);
    }

    public Platform getPlatform() {
        String platformId = applicationProperties.getPlatformId();
        switch (platformId.toLowerCase()) {
            case "jira": return Platform.Jira;
            case "conf": return Platform.Confluence;
            case "bitbucket": return Platform.Bitbucket;
            case "bamboo": return Platform.Bamboo;
            default: return Platform.Unknown;
        }
    }

    public String getPlatformName() {
        Platform platform = this.getPlatform();
        return platform.name();
    }

}
