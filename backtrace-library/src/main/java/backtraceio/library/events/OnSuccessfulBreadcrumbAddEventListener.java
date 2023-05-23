package backtraceio.library.events;

/**
 * Interface definition for a callback to be invoked after successfully storing a new breadcrumb.
 */
public interface OnSuccessfulBreadcrumbAddEventListener {
    /**
     * Event which will be executed after successfully storing a new breadcrumb.
     *
     * @param breadcrumbId the new breadcrumb id.
     */
    void onSuccessfulAdd(long breadcrumbId);
}
