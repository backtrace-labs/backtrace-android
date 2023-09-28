package backtraceio.library.models.json.naming;

public class NamingPolicy implements NamingConverter {
    NamingConverter instance;

    public NamingPolicy() {
        this(new LowerCaseWithDashConverter());
    }

    public NamingPolicy(NamingConverter namingConverter) {
        this.instance = namingConverter;
    }

    @Override
    public String convert(String value) {
        return instance.convert(value);
    }
}
