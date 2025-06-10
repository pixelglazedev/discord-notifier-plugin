package nz.co.jammehcow.jenkinsdiscord;

import static nz.co.jammehcow.jenkinsdiscord.DiscordWebhook.DESCRIPTION_LIMIT;
import static nz.co.jammehcow.jenkinsdiscord.DiscordWebhook.FOOTER_LIMIT;
import static nz.co.jammehcow.jenkinsdiscord.DiscordWebhook.StatusColor;
import static nz.co.jammehcow.jenkinsdiscord.DiscordWebhook.TITLE_LIMIT;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import javax.inject.Inject;
import jenkins.model.JenkinsLocationConfiguration;
import nz.co.jammehcow.jenkinsdiscord.exception.WebhookException;
import nz.co.jammehcow.jenkinsdiscord.util.EmbedDescription;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class DiscordPipelineStep extends AbstractStepImpl {
    private final String webhookURL;

    private String title;
    private String link;
    private String description;
    private String footer;
    private String image;
    private String thumbnail;
    private String result;
    private String notes;
    private String customAvatarUrl;
    private String customUsername;
    private DynamicFieldContainer dynamicFieldContainer;
    private boolean successful;
    private boolean unstable;
    private boolean enableArtifactsList;
    private boolean showChangeset;
    private String scmWebUrl;

    @DataBoundConstructor
    public DiscordPipelineStep(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public String getWebhookURL() {
        return webhookURL;
    }

    public String getTitle() {
        return title;
    }

    @DataBoundSetter
    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    @DataBoundSetter
    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    public String getFooter() {
        return footer;
    }

    @DataBoundSetter
    public void setFooter(String footer) {
        this.footer = footer;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @DataBoundSetter
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public boolean isUnstable() {
        return unstable;
    }

    @DataBoundSetter
    public void setUnstable(boolean unstable) {
        this.unstable = unstable;
    }

    @DataBoundSetter
    public void setImage(String url) {
        this.image = url;
    }

    public String getImage() {
        return image;
    }

    @DataBoundSetter
    public void setThumbnail(String url) {
        this.thumbnail = url;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    @DataBoundSetter
    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    @DataBoundSetter
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    @DataBoundSetter
    public void setCustomAvatarUrl(String customAvatarUrl) {
        this.customAvatarUrl = customAvatarUrl;
    }

    public String getCustomAvatarUrl() {
        return customAvatarUrl;
    }

    @DataBoundSetter
    public void setCustomUsername(String customUsername) {
        this.customUsername = customUsername;
    }

    public String getCustomUsername() {
        return customUsername;
    }

    @DataBoundSetter
    public void setEnableArtifactsList(boolean enable) {
        this.enableArtifactsList = enable;
    }

    public boolean getEnableArtifactsList() {
        return enableArtifactsList;
    }

    @DataBoundSetter
    public void setShowChangeset(boolean show) {
        this.showChangeset = show;
    }

    public boolean getShowChangeset() {
        return showChangeset;
    }

    @DataBoundSetter
    public void setScmWebUrl(String url) {
        this.scmWebUrl = url;
    }

    public String getScmWebUrl() {
        return scmWebUrl;
    }

    @DataBoundSetter
    public void setDynamicFieldContainer(String fieldsString) {
        this.dynamicFieldContainer = DynamicFieldContainer.of(fieldsString);
    }

    public String getDynamicFieldContainer() {
        if(dynamicFieldContainer == null){
            return "";
        }
        return dynamicFieldContainer.toString();
    }

    public static class DiscordPipelineStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        @Inject
        transient DiscordPipelineStep step;

        @StepContextParameter
        transient TaskListener listener;

        @Override
        protected Void run() throws Exception {
            listener.getLogger().println("Sending notification to Discord.");

            DiscordWebhook.StatusColor statusColor;
            statusColor = StatusColor.YELLOW;
            if (step.getResult() == null) {
                if (step.isSuccessful()) statusColor = DiscordWebhook.StatusColor.GREEN;
                if (step.isSuccessful() && step.isUnstable()) statusColor = DiscordWebhook.StatusColor.YELLOW;
                if (!step.isSuccessful() && !step.isUnstable()) statusColor = DiscordWebhook.StatusColor.RED;
            } else if (step.getResult().equals(Result.SUCCESS.toString())) {
                statusColor = StatusColor.GREEN;
            } else if (step.getResult().equals(Result.UNSTABLE.toString())) {
                statusColor = StatusColor.YELLOW;
            } else if (step.getResult().equals(Result.FAILURE.toString())) {
                statusColor = StatusColor.RED;
            } else if (step.getResult().equals(Result.ABORTED.toString())) {
                statusColor = StatusColor.GREY;
            } else {
                listener.getLogger().println(step.getResult() + " is not a valid result");
            }

            DiscordWebhook wh = new DiscordWebhook(step.getWebhookURL());
            wh.setTitle(checkLimitAndTruncate("title", step.getTitle(), TITLE_LIMIT));
            wh.setURL(step.getLink());
            wh.setThumbnail(step.getThumbnail());

            if (step.getEnableArtifactsList() || step.getShowChangeset()) {
                JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
                Run build = getContext().get(Run.class);
                wh.setDescription(new EmbedDescription(
                                build,
                                globalConfig,
                                step.getDescription(),
                                step.getEnableArtifactsList(),
                                step.getShowChangeset(),
                                step.getScmWebUrl()
                        ).toString()
                );
            } else {
                wh.setDescription(checkLimitAndTruncate("description", step.getDescription(), DESCRIPTION_LIMIT));
            }

            wh.setImage(step.getImage());
            wh.setFooter(checkLimitAndTruncate("footer", step.getFooter(), FOOTER_LIMIT));
            wh.setStatus(statusColor);
            wh.setContent(step.getNotes());

            if (step.getCustomAvatarUrl() != null) {
                wh.setCustomAvatarUrl(step.getCustomAvatarUrl());
            }

            if (step.getCustomUsername() != null) {
                wh.setCustomUsername(step.getCustomUsername());
            }

            // Add all key value field pairs to the webhook
            addDynamicFieldsToWebhook(wh);

            try {
                wh.send();
            } catch (WebhookException e) {
                e.printStackTrace(listener.getLogger());
            }

            return null;
        }

        /**
         * Add all key value field pairs to the webhook
         */
        private void addDynamicFieldsToWebhook(DiscordWebhook wh){
            // Early exit if we don't have any dynamicFieldContainer set
            if(step.dynamicFieldContainer == null){
                return;
            }
            // Go through all fields and add them to the webhook
            step.dynamicFieldContainer.getFields().forEach(pair -> wh.addField(pair.getKey(), pair.getValue(), false));
        }

        private String checkLimitAndTruncate(String fieldName, String value, int limit) {
            if (value == null) return "";
            if (value.length() > limit) {
                listener.getLogger().printf("Warning: '%s' field has more than %d characters (%d). It will be truncated.%n",
                        fieldName,
                        limit,
                        value.length());
                return value.substring(0, limit);
            }
            return value;
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() { super(DiscordPipelineStepExecution.class); }

        @Override
        public String getFunctionName() {
            return "discordSend";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Send an embed message to Webhook URL";
        }
    }
}
