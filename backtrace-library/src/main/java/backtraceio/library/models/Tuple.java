package backtraceio.library.models;

public class Tuple<T1, T2> {
    public final T1 first;
    public final T2 second;

    public Tuple(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }
}
