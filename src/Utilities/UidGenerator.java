package Utilities;

public class UidGenerator {

    long lastuid = 0;

    public String generateUID(int SID) {
	return generateUID(Integer.toString(SID));
    }

    public String generateUID(String SID) {
	char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	char[] res = new char[6];
	long last = lastuid;
	int len = chars.length;
	for (int i = 5; i > -1; i--) {
	    res[i] = chars[(int) (last % len)];
	    last = last / len;
	}
	lastuid++;
	// flipover
	if (lastuid == 308915776) {
	    lastuid = 0;
	}
	return SID + (new String(res));
    }
}
