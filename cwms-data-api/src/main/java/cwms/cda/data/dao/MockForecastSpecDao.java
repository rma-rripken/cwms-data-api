package cwms.cda.data.dao;

import static cwms.cda.api.Controllers.NOT_SUPPORTED_YET;

import cwms.cda.data.dto.TimeSeriesIdentifierDescriptor;
import cwms.cda.data.dto.forecast.ForecastSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/*
 * This class is a mock implementation of the ForecastSpecDao.
 */
public class MockForecastSpecDao extends ForecastSpecDao {

    public MockForecastSpecDao(DSLContext dsl) {
        super(dsl);
    }


    public void create(ForecastSpec forecastSpec) {

        return;
    }

    public void delete(String office, String specId, String designator) {
        return;
    }

    public List<ForecastSpec> getForecastSpecs(String office, String specIdRegex,
                                               String designator, String location,
                                               String sourceEntity) {
        List<ForecastSpec> retval = new ArrayList<>();

        retval.add(buildForecastSpec(office, "test_spec_1", designator,
                Arrays.asList("Spec1TestLocA.Flow.Inst.1Hour.0.raw","Spec1TestLocB.Flow.Inst.1Hour.0.raw"),
                Arrays.asList("Spec1TestLocA", "Spec1TestLocB")));
        retval.add(buildForecastSpec(office, "test_spec_2", designator,
                Arrays.asList("Spec2TestLocA.Flow.Inst.1Hour.0.raw",
                        "Spec2TestLocB.Flow.Inst.1Hour.0.raw", "Spec2TestLocB.Flow.Inst.1Hour.0.rew"),
                Arrays.asList("Spec2TestLocA", "Spec2TestLocB")));

        return retval;
    }

    public ForecastSpec getForecastSpec(String office, String name, String designator) {

        return buildForecastSpec(office, "test_spec_1", designator,
                Arrays.asList("Spec1TestLocA.Flow.Inst.1Hour.0.raw","Spec1TestLocB.Flow.Inst.1Hour.0.raw"),
                Arrays.asList("Spec1TestLocA", "Spec1TestLocB"));
    }

    public static ForecastSpec buildForecastSpec(String office, String name, String designator, List<String> tsIds, Collection<String> locationIds) {

        List<TimeSeriesIdentifierDescriptor> tsids = getTimeSeriesIdentifierDescriptors(tsIds);

        Set<String> locations = new LinkedHashSet<>();
        locations.addAll(locationIds);

        return new ForecastSpec.Builder()
                .withSpecId(name)
                .withOfficeId(office)
                .withDesignator(designator)
                .withLocationIds(locations)
                .withSourceEntityId("sourceEntity")
                .withDescription("description")
                .withTimeSeriesIds(tsids)
                .build();
    }

    @NotNull
    private static List<TimeSeriesIdentifierDescriptor> getTimeSeriesIdentifierDescriptors(List<String> tsIds) {
        List<TimeSeriesIdentifierDescriptor> tsids = new ArrayList<>();

        TimeSeriesIdentifierDescriptor.Builder builder = new TimeSeriesIdentifierDescriptor.Builder();
        builder.withOfficeId("office");

        for (String tsId : tsIds) {
            tsids.add(builder.withTimeSeriesId(tsId).build());
        }
        return tsids;
    }

    public void update(ForecastSpec forecastSpec) {
        return;
    }
}
