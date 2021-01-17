package il.ac.idc.cs.sinkhole;

public class SinkholeServer {

    public static void main(String[] args) {
        DNSRecursiveServer dnsRecursiveServer = null;
        if (args.length > 0) {
            dnsRecursiveServer = new DNSRecursiveServer(args[0]);
        } else {
            dnsRecursiveServer = new DNSRecursiveServer();
        }
        dnsRecursiveServer.Run();
    }
}
