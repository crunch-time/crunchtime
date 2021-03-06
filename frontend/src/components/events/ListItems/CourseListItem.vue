<template>
  <EventListItem
    :title="course.fullCourseNumber"
    :borderColor="borderColor"
    :backgroundColor="backgroundColor"
  >
    <template v-slot:subtext>
      <strong>{{ displayUnits }}</strong>
      &middot; {{ course.description.substring(0,25)+'...' }}
    </template>
    <template v-slot:popoverContent>
      <div v-if="Object.getOwnPropertyNames(groupedRequirements).length != 0">
        <div v-for="[college, ges] in Object.entries(groupedRequirements)" v-bind:key="college">
          <strong>{{ college }} GEs: </strong><span v-for="ge in ges.sort()" v-bind:key="ge.geCode">{{ ge['geCode'].trim() }}, </span>
        </div>
      </div>
      <div v-else>
        <strong>GE's: </strong> None
      </div>
      <strong>Units: </strong>{{ displayUnits }}
      <br />
      <strong>College: </strong>{{ course.college }}
      <br />
      <strong>Description: </strong><small>{{ course.description }}</small>
    </template>
    <template v-slot:buttons>
      <slot name="buttons"></slot>
    </template>
  </EventListItem>
</template>

<script>
import EventListItem from "@/components/events/ListItems/EventListItem.vue";
import { getBackgroundColor, getBorderColor } from "@/components/util/color-utils.js";
import { groupBy } from "@/components/util/util-methods.js";

export default {
  props: {
    course: {
      type: Object
    }
  },
  components: {
    EventListItem
  },
  computed: {
    /**
     * Returns a string with the border color for the event
     */
    borderColor: function() {
      // TODO: Once we have class definitions on the frontend, consolidate any usages of "getColor" stuff to the class definitions.
      return getBorderColor(this.course.deptCode);
    },
    /**
     * Returns a string with the border color for the event
     */
    backgroundColor: function() {
      // TODO: Once we have class definitions on the frontend, consolidate any usages of "getColor" stuff to the class definitions.
      return getBackgroundColor(this.course.courseId.slice(7, 14));
    },
    /**
     * Returns the requirements fulfilled by the course, grouped by the college.
     */
    groupedRequirements: function() {
      if (this.course.generalEducation) {
        return groupBy(this.course.generalEducation, a => a.geCollege);
      } else {
        return {};
      }
    },
    /**
     * Returns display text for the units. Some courses have variable units, others are fixed.
     */
    displayUnits: function() {
      if (this.course.unitsFixed) {
        return this.course.unitsFixed + " units";
      } else {
        return (
          this.course.unitsVariableLow +
          " to " +
          this.course.unitsVariableHigh +
          " units"
        );
      }
    }
  }
};
</script>

<style>
</style>