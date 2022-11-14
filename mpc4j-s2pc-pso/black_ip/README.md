# Black IP Dataset

Private set union (PSU) enables two parties, each holding a  private set of elements, to compute the union of the two sets while revealing nothing more than the union itself. One important application of PSU is blacklist and vulnerability data aggregation. Consider that there are two organizations (i.e. the maintainers of the IP blacklists) who want to compute their IP blacklist joint list, which will help minimize vulnerabilities in their infrastructure. 

We run PSU experiments on a black IP dataset to demonstrate this PSU application. The black IP dataset is available at **[BlackIP](https://github.com/maravento/blackip)**. In our experiment, we assume the PSU sender maintains `blackip.txt` (with 3,176,636 distinct IPs), and the PSU client maintains `oldip.txt` (with 2,514,551 distinct IPs). The union result contains 3,178,512 IPs. All IPs in `blackip.txt` and `oldip.txt` are IPv4 addresses. Each IP is a 32-bit number, written in decimal digits and formatted as four 8-bit fields separated by periods. In our experiments, we uniquely represent each of these IPs by a 32-bit binary string. The correlated configuration files are in `conf/psu_black_ip`.

# About BlackIP

The descriptions below are from [READMD.md](https://github.com/maravento/blackip.README.md) in the root of the BlackIP project.

**BlackIP** is a project that collects and unifies public blocklists of IP addresses, to make them compatible with [Squid](http://www.squid-cache.org/) and [IPSET](http://ipset.netfilter.org/) ([Iptables](http://www.netfilter.org/documentation/HOWTO/es/packet-filtering-HOWTO-7.html) [Netfilter](http://www.netfilter.org/))

**BlackIP** es un proyecto que recopila y unifica listas públicas de bloqueo de direcciones IPs, para hacerlas compatibles con [Squid](http://www.squid-cache.org/) e [IPSET](http://ipset.netfilter.org/) ([Iptables](http://www.netfilter.org/documentation/HOWTO/es/packet-filtering-HOWTO-7.html) [Netfilter](http://www.netfilter.org/))


## DATA SHEET

|ACL|Blocked IP|File Size|
| :---: | :---: | :---: |
|blackip.txt|3176744|45,4 Mb|

## GIT CLONE

```bash
git clone https://github.com/maravento/blackip.git
```

## CONTRIBUTIONS

We thank all those who contributed to this project. Those interested may contribute sending us new "Blocklist" links to be included in this project / Agradecemos a todos aquellos que han contribuido a este proyecto. Los interesados pueden contribuir, enviándonos enlaces de nuevas "Blocklist", para ser incluidas en este proyecto

Special thanks to: [Jhonatan Sneider](https://github.com/sney2002)

## DONATE

BTC: 3M84UKpz8AwwPADiYGQjT9spPKCvbqm4Bc

## BUILD

[![CreativeCommons](https://licensebuttons.net/l/by-sa/4.0/88x31.png)](http://creativecommons.org/licenses/by-sa/4.0/)
[maravento.com](http://www.maravento.com) is licensed under a [Creative Commons Reconocimiento-CompartirIgual 4.0 Internacional License](http://creativecommons.org/licenses/by-sa/4.0/).

## OBJECTION

Due to recent arbitrary changes in computer terminology, it is necessary to clarify the meaning and connotation of the term **blacklist**, associated with this project: *In computing, a blacklist, denylist or blocklist is a basic access control mechanism that allows through all elements (email addresses, users, passwords, URLs, IP addresses, domain names, file hashes, etc.), except those explicitly mentioned. Those items on the list are denied access. The opposite is a whitelist, which means only items on the list are let through whatever gate is being used.*

Debido a los recientes cambios arbitrarios en la terminología informática, es necesario aclarar el significado y connotación del término **blacklist**, asociado a este proyecto: *En informática, una lista negra, lista de denegación o lista de bloqueo es un mecanismo básico de control de acceso que permite a través de todos los elementos (direcciones de correo electrónico, usuarios, contraseñas, URL, direcciones IP, nombres de dominio, hashes de archivos, etc.), excepto los mencionados explícitamente. Esos elementos en la lista tienen acceso denegado. Lo opuesto es una lista blanca, lo que significa que solo los elementos de la lista pueden pasar por cualquier puerta que se esté utilizando.*

Source [Wikipedia](https://en.wikipedia.org/wiki/Blacklist_(computing))

Therefore / Por tanto

**blacklist**, **blocklist**, **blackweb**, **blackip**, **whitelist**, **etc.**

are terms that have nothing to do with racial discrimination / son términos que no tienen ninguna relación con la discriminación racial

## DISCLAIMER

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
