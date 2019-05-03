package me.symlab.kirill.sparthanmobile.Utils;

public class SettingsStore {

    private static SettingsStore mInstance = null;

    public static final int MIN_GESTURES = 2;
    public static final int MAX_GESTURES = 10;

    public enum Connectivity {
        HTTP,
        TCP,
        UDP
    }

    public enum Classifiers {
        CLASSIC,
        GRAB,
        FULL
    }

    Connectivity connectivity;
    int numGestures;
    boolean manual;
    boolean useCloud;

    protected SettingsStore() {
        connectivity = Connectivity.HTTP;
        numGestures = 5;
        manual = false;
        useCloud = false;
    }

    public static synchronized SettingsStore getInstance() {
        if (null == mInstance) {
            mInstance = new SettingsStore();
        }
        return mInstance;
    }

    public Connectivity getConnectivity() {
        return connectivity;
    }

    public void setConnectivity(Connectivity connectivity) {
        this.connectivity = connectivity;
    }

    public int getNumGestures() {
        return numGestures;
    }

    public void setNumGestures(int numGestures) {
        if (numGestures < MIN_GESTURES) {
            numGestures = MIN_GESTURES;
        }
        if (numGestures > MAX_GESTURES) {
            numGestures = MAX_GESTURES;
        }
        this.numGestures = numGestures;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public boolean useCloud() {
        return useCloud;
    }

    public void setUseCloud(boolean useCloud) {
        this.useCloud = useCloud;
    }
}