package com.Jdbye.BukkitIRCd;

public class Main {

	static IRCd ircd = null;

	public static void main(String[] args) {
		ircd = new IRCd();
		Thread thr = new Thread(ircd);
		thr.start();
	}
}
