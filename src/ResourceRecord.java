
public class ResourceRecord {
    private String name;
    private int type;
    private String rdData;

    public ResourceRecord(String name, int type, String rdData) {
        this.name = name;
        this.type = type;
        this.rdData = rdData;
    }

    public String getName()
    {
        return this.name;
    }
    public int getType()
    {
        return this.type;
    }
    public String getRDData()
    {
        return this.rdData;
    }

}
