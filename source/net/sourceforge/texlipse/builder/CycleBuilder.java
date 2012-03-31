package net.sourceforge.texlipse.builder;

public interface CycleBuilder {

    /**
     * Returns the cycle detector of this builder instance.
     *
     * @return cycle detector
     */
    public BuildCycleDetector getCycleDetector();

    /**
     * Sets the cycle detector, which determines how often latex and other
     * runners need to be triggered. In case of sequenced runners, this should
     * be called after updating and validating builder settings.
     *
     * @param cycleDetector cycle detector instance
     */
    public void setCycleDetector(BuildCycleDetector cycleDetector);

}
