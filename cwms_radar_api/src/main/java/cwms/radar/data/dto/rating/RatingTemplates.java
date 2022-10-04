package cwms.radar.data.dto.rating;

import cwms.radar.api.errors.FieldException;
import cwms.radar.data.dto.CwmsDTOPaginated;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RatingTemplates extends CwmsDTOPaginated {

    private List<RatingTemplate> templates;

    private RatingTemplates() {
    }

    private int offset;

    private RatingTemplates(int offset, int pageSize, Integer total,
                            List<RatingTemplate> templates) {
        super(Integer.toString(offset), pageSize, total);
        this.templates = new ArrayList<>(templates);
        this.offset = offset;
    }

    public List<RatingTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }


    @Override
    public void validate() throws FieldException {

    }

    public static class Builder {
        private final int offset;
        private final int pageSize;
        private final Integer total;
        private List<RatingTemplate> templates;

        public Builder(int offset, int pageSize, Integer total) {
            this.offset = offset;
            this.pageSize = pageSize;
            this.total = total;
        }

        public Builder templates(List<RatingTemplate> specList) {
            this.templates = specList;
            return this;
        }

        private int getRatingCount() {
            int count = 0;

            if(templates != null) {
                for (RatingTemplate template : templates) {
                    List<String> ratingIds = template.getRatingIds();
                    if (ratingIds != null) {
                        count += ratingIds.size();
                    }
                }
            }

            return count;
        }

        public RatingTemplates build() {
            RatingTemplates retval = new RatingTemplates(offset, pageSize, total, templates);

            int count = getRatingCount();
            if (count >= this.pageSize) {
                String cursor = Integer.toString(retval.offset + count);
                retval.nextPage = encodeCursor(cursor, retval.pageSize, retval.total);
            } else {
                retval.nextPage = null;
            }
            return retval;
        }

    }

}
