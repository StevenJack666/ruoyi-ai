package org.ruoyi.common.core.utils.openapi.sm3;

import org.ruoyi.common.core.utils.openapi.sm3.child.GeneralDigest;
import org.ruoyi.common.core.utils.openapi.sm3.child.Pack;

public class SM3Digest extends GeneralDigest {

	private static final int DIGEST_LENGTH = 32;

	private int[] x = new int[68];
	private int[] y = new int[64];
	private int xOff;
	private int h1;
	private int h2;
	private int h3;
	private int h4;
	private int h5;
	private int h6;
	private int h7;
	private int h8;

	public SM3Digest() {
		reset();
	}

	protected void processBlock() {
		for (int i = 16; i < 68; i++) {
			this.x[i] = (p1(this.x[(i - 16)] ^ this.x[(i - 9)]
					^ (this.x[(i - 3)] << 15 | this.x[(i - 3)] >>> 17))
					^ (this.x[(i - 13)] << 7 | this.x[(i - 13)] >>> 25) ^ this.x[(i - 6)]);
		}

		for (int j = 0; j < 64; j++) {
			this.y[j] = (this.x[j] ^ this.x[(j + 4)]);
		}

		int A = this.h1;
		int B = this.h2;
		int C = this.h3;
		int D = this.h4;
		int E = this.h5;
		int F = this.h6;
		int G = this.h7;
		int H = this.h8;

		int tempSS1 = 0;
		int SS1 = 0;
		int SS2 = 0;
		int TT1 = 0;
		int TT2 = 0;
		for (int i = 0; i < 16; i++) {
			tempSS1 = (A << 12 | A >>> 20) + E
					+ (2043430169 << i | 2043430169 >>> 32 - i);
			SS1 = tempSS1 << 7 | tempSS1 >>> 25;
			SS2 = SS1 ^ (A << 12 | A >>> 20);
			TT1 = f1(A, B, C) + D + SS2 + this.y[i];
			TT2 = g1(E, F, G) + H + SS1 + this.x[i];
			D = C;
			C = B << 9 | B >>> 23;
			B = A;
			A = TT1;
			H = G;
			G = F << 19 | F >>> 13;
			F = E;
			E = p0(TT2);
		}

		for (int j = 16; j < 64; j++) {
			tempSS1 = (A << 12 | A >>> 20) + E
					+ (2055708042 << j | 2055708042 >>> 32 - j);
			SS1 = tempSS1 << 7 | tempSS1 >>> 25;
			SS2 = SS1 ^ (A << 12 | A >>> 20);
			TT1 = f2(A, B, C) + D + SS2 + this.y[j];
			TT2 = g2(E, F, G) + H + SS1 + this.x[j];
			D = C;
			C = B << 9 | B >>> 23;
			B = A;
			A = TT1;
			H = G;
			G = F << 19 | F >>> 13;
			F = E;
			E = p0(TT2);
		}

		this.h1 ^= A;
		this.h2 ^= B;
		this.h3 ^= C;
		this.h4 ^= D;
		this.h5 ^= E;
		this.h6 ^= F;
		this.h7 ^= G;
		this.h8 ^= H;

		this.xOff = 0;
		for (int i = 0; i < 16; i++) {
			this.x[i] = 0;
		}
	}

	protected void processLength(long bitLength) {
		if (this.xOff > 14) {
			processBlock();
		}

		this.x[14] = ((int) (bitLength >>> 32));
		this.x[15] = ((int) (bitLength & 0xFFFFFFFF));
	}

	protected void processWord(byte[] in, int inOff) {
		int n = in[inOff] << 24;
		n |= (in[(++inOff)] & 0xFF) << 16;
		n |= (in[(++inOff)] & 0xFF) << 8;
		n |= in[(++inOff)] & 0xFF;
		this.x[this.xOff] = n;

		if (++this.xOff == 16) {
			processBlock();
		}
	}

	public int doFinal(byte[] out, int outOff) {
		finish();

		Pack.intToBigEndian(this.h1, out, outOff);
		Pack.intToBigEndian(this.h2, out, outOff + 4);
		Pack.intToBigEndian(this.h3, out, outOff + 8);
		Pack.intToBigEndian(this.h4, out, outOff + 12);
		Pack.intToBigEndian(this.h5, out, outOff + 16);
		Pack.intToBigEndian(this.h6, out, outOff + 20);
		Pack.intToBigEndian(this.h7, out, outOff + 24);
		Pack.intToBigEndian(this.h8, out, outOff + 28);

		reset();

		return DIGEST_LENGTH;
	}

	public String getAlgorithmName() {
		return "SM3";
	}

	public int getDigestSize() {
		return DIGEST_LENGTH;
	}

	private int p0(int x) {
		return x ^ (x << 9 | x >>> 23) ^ (x << 17 | x >>> 15);
	}

	private int p1(int x) {
		return x ^ (x << 15 | x >>> 17) ^ (x << 23 | x >>> 9);
	}

	private int f1(int x, int y, int z) {
		return x ^ y ^ z;
	}

	private int f2(int x, int y, int z) {
		return x & y | x & z | y & z;
	}

	private int g1(int x, int y, int z) {
		return x ^ y ^ z;
	}

	private int g2(int x, int y, int z) {
		return x & y | (x ^ 0xFFFFFFFF) & z;
	}

	public void reset() {
		super.reset();

		this.h1 = 1937774191;
		this.h2 = 1226093241;
		this.h3 = 388252375;
		this.h4 = -628488704;
		this.h5 = -1452330820;
		this.h6 = 372324522;
		this.h7 = -477237683;
		this.h8 = -1325724082;

		this.xOff = 0;
		for (int i = 0; i != this.x.length; i++) {
			this.x[i] = 0;
		}
		for (int j = 0; j != this.y.length; j++){
			this.y[j] = 0;
		}
	}
}
