package uk.org.openseizuredetector.locator;

interface LocationReceiver {
    /** The function to be called once we have found the location */
    public void onLocationFound(LonLat ll);
}
