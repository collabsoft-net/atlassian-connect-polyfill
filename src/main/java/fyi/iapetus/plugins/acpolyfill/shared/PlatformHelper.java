package fyi.iapetus.plugins.acpolyfill.shared;

import com.atlassian.sal.api.ApplicationProperties;

public class PlatformHelper {

    private final ApplicationProperties applicationProperties;

    public PlatformHelper(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String getPlatformName() {
        String platformId = applicationProperties.getPlatformId();
        switch (platformId.toLowerCase()) {
            case "jira": return "Jira";
            case "conf": return "Confluence";
            case "bitbucket": return "Bitbucket";
            case "bamboo": return "Bamboo";
            default: return "";
        }
    }

}
