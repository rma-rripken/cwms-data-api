package cwms.cda.data.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import cwms.cda.api.errors.FieldException;
import cwms.cda.formatters.Formats;
import cwms.cda.formatters.annotations.FormattableWith;
import cwms.cda.formatters.json.JsonV2;
import java.time.Instant;

@JsonDeserialize(builder = Project.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
@FormattableWith(contentType = Formats.JSONV2, formatter = JsonV2.class)
public class Project extends CwmsDTO {

    private final String name;
    private final Double federalCost;
    private final Double nonFederalCost;
    private final Instant costYear;
    private final String costUnit;
    private final Double federalOAndMCost;
    private final Double nonFederalOAndMCost;
    private final String authorizingLaw;
    private final String projectOwner;
    private final String hydropowerDesc;
    private final String sedimentationDesc;
    private final String downstreamUrbanDesc;
    private final String bankFullCapacityDesc;
    private final String pumpBackLocationId;
    private final String pumpBackOfficeId;
    private final String nearGageLocationId;
    private final String nearGageOfficeId;
    private final Instant yieldTimeFrameStart;
    private final Instant yieldTimeFrameEnd;
    private final String projectRemarks;


    private Project(Project.Builder builder) {
        super(builder.officeId);
        this.name = builder.name;
        this.federalCost = builder.federalCost;
        this.nonFederalCost = builder.nonFederalCost;
        this.costYear = builder.costYear;
        this.costUnit = builder.costUnit;
        this.federalOAndMCost = builder.federalOAndMCost;
        this.nonFederalOAndMCost = builder.nonFederalOandMCost;
        this.authorizingLaw = builder.authorizingLaw;
        this.projectOwner = builder.projectOwner;
        this.hydropowerDesc = builder.hydropowerDesc;
        this.sedimentationDesc = builder.sedimentationDesc;
        this.downstreamUrbanDesc = builder.downstreamUrbanDesc;
        this.bankFullCapacityDesc = builder.bankFullCapacityDesc;
        this.pumpBackLocationId = builder.pumpBackLocationId;
        this.pumpBackOfficeId = builder.pumpBackOfficeId;
        this.nearGageLocationId = builder.nearGageLocationId;
        this.nearGageOfficeId = builder.nearGageOfficeId;
        this.yieldTimeFrameStart = builder.yieldTimeFrameStart;
        this.yieldTimeFrameEnd = builder.yieldTimeFrameEnd;
        this.projectRemarks = builder.projectRemarks;
    }

    @Override
    public void validate() throws FieldException {

    }

    public String getAuthorizingLaw() {
        return authorizingLaw;
    }

    public String getBankFullCapacityDesc() {
        return bankFullCapacityDesc;
    }

    public String getDownstreamUrbanDesc() {
        return downstreamUrbanDesc;
    }

    public Double getFederalCost() {
        return federalCost;
    }

    public String getHydropowerDesc() {
        return hydropowerDesc;
    }

    public String getNearGageLocationId() {
        return nearGageLocationId;
    }

    public String getNearGageOfficeId() {
        return nearGageOfficeId;
    }

    public Double getNonFederalCost() {
        return nonFederalCost;
    }

    public Double getFederalOAndMCost() {
        return federalOAndMCost;
    }

    public Double getNonFederalOAndMCost() {
        return nonFederalOAndMCost;
    }

    public Instant getCostYear() {
        return costYear;
    }

    public String getCostUnit() {
        return costUnit;
    }

    public String getName() {
        return name;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public String getProjectRemarks() {
        return projectRemarks;
    }

    public String getPumpBackLocationId() {
        return pumpBackLocationId;
    }

    public String getPumpBackOfficeId() {
        return pumpBackOfficeId;
    }

    public String getSedimentationDesc() {
        return sedimentationDesc;
    }

    public Instant getYieldTimeFrameEnd() {
        return yieldTimeFrameEnd;
    }

    public Instant getYieldTimeFrameStart() {
        return yieldTimeFrameStart;
    }

    @JsonPOJOBuilder
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Builder {
        private String officeId;
        private String name;
        private Double federalCost;
        private Double nonFederalCost;
        private Instant costYear;
        private String costUnit;
        private Double federalOAndMCost;
        private Double nonFederalOandMCost;
        private String authorizingLaw;
        private String projectOwner;
        private String hydropowerDesc;
        private String sedimentationDesc;
        private String downstreamUrbanDesc;
        private String bankFullCapacityDesc;
        private String pumpBackLocationId;
        private String pumpBackOfficeId;
        private String nearGageLocationId;
        private String nearGageOfficeId;
        private Instant yieldTimeFrameStart;
        private Instant yieldTimeFrameEnd;
        private String projectRemarks;


        public Project build() {
            return new Project(this);
        }

        /**
         * Copy the values from the given project into this builder.
         * @param project the project to copy values from
         * @return this builder
         */
        public Builder from(Project project) {
            return this.withOfficeId(project.getOfficeId())
                    .withName(project.getName())
                    .withFederalCost(project.getFederalCost())
                    .withNonFederalCost(project.getNonFederalCost())
                    .withCostYear(project.getCostYear())
                    .withCostUnit(project.getCostUnit())
                    .withFederalOAndMCost(project.getFederalOAndMCost())
                    .withNonFederalOAndMCost(project.getNonFederalOAndMCost())
                    .withAuthorizingLaw(project.getAuthorizingLaw())
                    .withProjectOwner(project.getProjectOwner())
                    .withHydropowerDesc(project.getHydropowerDesc())
                    .withSedimentationDesc(project.getSedimentationDesc())
                    .withDownstreamUrbanDesc(project.getDownstreamUrbanDesc())
                    .withBankFullCapacityDesc(project.getBankFullCapacityDesc())
                    .withPumpBackLocationId(project.getPumpBackLocationId())
                    .withPumpBackOfficeId(project.getPumpBackOfficeId())
                    .withNearGageLocationId(project.getNearGageLocationId())
                    .withNearGageOfficeId(project.getNearGageOfficeId())
                    .withYieldTimeFrameStart(project.getYieldTimeFrameStart())
                    .withYieldTimeFrameEnd(project.getYieldTimeFrameEnd())
                    .withProjectRemarks(project.getProjectRemarks());
        }

        public Builder withOfficeId(String officeId) {
            this.officeId = officeId;
            return this;
        }

        public Builder withName(String projectId) {
            this.name = projectId;
            return this;
        }

        public Builder withFederalCost(Double federalCost) {
            this.federalCost = federalCost;
            return this;
        }

        public Builder withNonFederalCost(Double nonFederalCost) {
            this.nonFederalCost = nonFederalCost;
            return this;
        }

        public Builder withCostYear(Instant costYear) {
            this.costYear = costYear;
            return this;
        }

        public Builder withCostUnit(String costUnit) {
            this.costUnit = costUnit;
            return this;
        }

        public Builder withFederalOAndMCost(Double federalOandMCost) {
            this.federalOAndMCost = federalOandMCost;
            return this;
        }

        public Builder withNonFederalOAndMCost(Double nonFederalOandMCost) {
            this.nonFederalOandMCost = nonFederalOandMCost;
            return this;
        }

        public Builder withAuthorizingLaw(String authorizingLaw) {
            this.authorizingLaw = authorizingLaw;
            return this;
        }

        public Builder withProjectOwner(String projectOwner) {
            this.projectOwner = projectOwner;
            return this;
        }

        public Builder withHydropowerDesc(String hydropowerDesc) {
            this.hydropowerDesc = hydropowerDesc;
            return this;
        }

        public Builder withSedimentationDesc(String sedimentationDesc) {
            this.sedimentationDesc = sedimentationDesc;
            return this;
        }

        public Builder withDownstreamUrbanDesc(String downstreamUrbanDesc) {
            this.downstreamUrbanDesc = downstreamUrbanDesc;
            return this;
        }

        public Builder withBankFullCapacityDesc(String bankFullCapacityDesc) {
            this.bankFullCapacityDesc = bankFullCapacityDesc;
            return this;
        }

        public Builder withPumpBackLocationId(String pumpBackLocationId) {
            this.pumpBackLocationId = pumpBackLocationId;
            return this;
        }

        public Builder withNearGageLocationId(String nearGageLocationId) {
            this.nearGageLocationId = nearGageLocationId;
            return this;
        }

        public Builder withNearGageOfficeId(String nearGageOfficeId) {
            this.nearGageOfficeId = nearGageOfficeId;
            return this;
        }

        public Builder withYieldTimeFrameStart(Instant yieldTimeFrameStart) {
            this.yieldTimeFrameStart = yieldTimeFrameStart;
            return this;
        }

        public Builder withYieldTimeFrameEnd(Instant yieldTimeFrameEnd) {
            this.yieldTimeFrameEnd = yieldTimeFrameEnd;
            return this;
        }

        public Builder withProjectRemarks(String projectRemarks) {
            this.projectRemarks = projectRemarks;
            return this;
        }

        public Builder withPumpBackOfficeId(String spk) {
            this.pumpBackOfficeId = spk;
            return this;
        }
    }

}