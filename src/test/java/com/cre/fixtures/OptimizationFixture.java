package com.cre.fixtures;

/**
 * Fixture class for testing output optimization and comment stripping.
 * This class level Javadoc should be preserved when this class is the target.
 */
public class OptimizationFixture {

    private String name;

    /**
     * Constructor for OptimizationFixture.
     * This Javadoc should be pruned if this is a neighbor skeleton.
     */
    public OptimizationFixture(String name) {
        // Internal comment in constructor - should be pruned if skeleton
        this.name = name;
    }

    /**
     * Gets the name of the fixture.
     * Javadoc here should be preserved if this method is the target.
     * It should be pruned if it's a neighbor skeleton.
     * @return the name
     */
    public String getName() {
        // Internal comment in getName - should be pruned if skeleton
        return name;
    }

    /**
     * Performs an optimized action.
     * Javadoc should be preserved if target.
     */
    public void performAction() {
        // Step 1: Initialize
        System.out.println("Action started");

        /*
         * Multi-line internal comment
         * that should be stripped in neighbors
         */
        System.out.println("Action performing");

        // Step 2: Finish
        System.out.println("Action finished");
    }
}
