# DNS-Sinkhole
A Java implementation of a recursive DNS sinkhole server that returns a false result for pre-defined blocked domains.

<h2> Project Structure </h2>

<code>SinkholeServer</code> - The server's entry point (including main function)

<code>DNSRecursiveServer</code> -  The main class for the DNS RecursiveServer

<code>DNSQuery</code> - A class representing and analyzing DNS query bytes

<code>BlockListFilter</code> - Stores a HashSet of domains to be blocked from the input txt file and validates if a given domain should be blocked.

<code>Question</code> - Represents a DNS Question record.

<code>ResourceRecord</code> - Represents a DNS Resource record (Answer / Authority).

<code>BytesOperations</code> - Helper class with useful bit-wise operations on bytes.

<h2> Credits </h2>

<b>[Tomer Ben-Gigi](https://github.com/tomerbg2911)</b> & <b>[Etamar Romano](https://github.com/EtamarRomano)</b>

IDC (Interdisciplinary Center Herzliya)

<h2> TODO (someday, maybe) </h2>

- Implement a fully independent iterative resolver, I.e. not relying on local resolver for intermediate DNS server names

- Add some level of answers caching

- Handling network issues - handle a query/response to root/intermediate DNS being lost.

- Reacting more error types (we now only support RCODE=3).





