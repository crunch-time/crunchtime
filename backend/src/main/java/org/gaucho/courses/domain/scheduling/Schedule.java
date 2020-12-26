package org.gaucho.courses.domain.scheduling;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.gaucho.courses.domain.remote.ClassSection;
import org.gaucho.courses.domain.core.Event;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import java.io.Serializable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@Slf4j
@Entity(name = "schedules")
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Fluent builder class to generate a Schedule.
     */
    @Data
    @Accessors(chain=true)
    public static class ScheduleBuilder {

        /**
         * Constructor. Quarter and useremail are required properties.
         * @param quarter The academic quarter.
         * @param userEmail The user's email.
         */
        public ScheduleBuilder(String quarter, String userEmail) {
            this.setQuarter(quarter)
                .setUserEmail(userEmail);
        }

        private String quarter;
        private String userEmail;
        private String name;
        private int totalUnits;
        private List<ClassSection> selectedClasses = new ArrayList<>();
        private List<ClassSection> scheduledClasses = new ArrayList<>();
        private List<CustomEvent> customEvents = new ArrayList<>();

        /**
         * Builds a schedule given the properties set earlier.
         * @return A Schedule object.
         */
        public Schedule build() {
            return new Schedule(this);
        }
    }

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @OneToMany(
        cascade = CascadeType.PERSIST)
    private List<CourseAndClassIds> classes = new ArrayList<>();

    @ManyToMany(
        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.EAGER)
    private List<CustomEvent> customEvents = new ArrayList<CustomEvent>();

    @Embedded
    private ScheduleSortingAttributes sortingAttributes;

    @NotNull private String quarter;
    @NotNull private String userEmail;
    private String name;
    private int totalUnits;
    private Boolean conflicting;

    /**
     * Constructor. For user with the ScheduleBuilder inner class.
     * @param builder A schedule builder object.
     */
    public Schedule(ScheduleBuilder builder) {
        this.setQuarter(builder.getQuarter());
        this.setUserEmail(builder.getUserEmail());
        this.setName(builder.getName());
        this.setTotalUnits(builder.getTotalUnits());
        this.setCustomEvents(builder.getCustomEvents());

        builder.getSelectedClasses().forEach((ClassSection section) -> {
            Optional<CourseAndClassIds> maybe = this.classes
                .stream()
                .filter(c-> c.courseId.equals(section.getCourseId()))
                .findFirst();
            if (maybe.isPresent()) {
                maybe.get().selectedEnrollCodes.add(section.getEnrollCode());
            } else {
                CourseAndClassIds ids = new CourseAndClassIds();
                ids.courseId = section.getCourseId();
                ids.selectedEnrollCodes.add(section.getEnrollCode());
                this.classes.add(ids);
            }
        });

        builder.getScheduledClasses().forEach((ClassSection section) -> {
            Optional<CourseAndClassIds> maybe = this.classes
                .stream()
                .filter(c -> c.courseId.equals(section.getCourseId()))
                .findFirst();
            if (maybe.isPresent()) {
                maybe.get().scheduledEnrollCodes.add(section.getEnrollCode());
            } else {
                CourseAndClassIds ids = new CourseAndClassIds();
                ids.courseId = section.getCourseId();
                ids.scheduledEnrollCodes.add(section.getEnrollCode());
                this.classes.add(ids);
            }
        });

        List<? extends Event> allEvents = Stream.of(
                builder.getScheduledClasses(),
                this.getCustomEvents())
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        this.setConflicting(Event.eventsHaveConflicts(allEvents));
        this.setSortingAttributes(new ScheduleSortingAttributes(allEvents));
    }

    private void setCustomEvents(List<CustomEvent> events) {
        if (events != null) {
            this.customEvents = events;
            this.customEvents.forEach((CustomEvent c) -> c.addSchedule(this));  // Set back-reference for JPA
        }
    }

}