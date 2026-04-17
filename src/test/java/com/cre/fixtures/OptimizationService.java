package com.cre.fixtures;

/**
 * Service that uses OptimizationFixture to test cross-file comment stripping.
 */
public class OptimizationService {

    private final OptimizationFixture fixture;

    /**
     * Constructor for OptimizationService.
     */
    public OptimizationService() {
        this.fixture = new OptimizationFixture("ServiceFixture");
    }

    /**
     * Executes the service logic by calling the fixture.
     * When this is the target, Javadoc should be preserved.
     * The neighboring methods like getFixture() should be skeletons.
     */
    public void executeService() {
        // Log starting
        System.out.println("Service executing");
        
        fixture.performAction();
        
        // Log finished
        System.out.println("Service finished");
    }

    /**
     * Returns the fixture used by the service.
     */
    public OptimizationFixture getFixture() {
        return fixture;
    }
}
