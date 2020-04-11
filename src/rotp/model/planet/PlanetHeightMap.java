/*
 * 
 * 
 * This file is a Java-rewrite of the "Planet Generator" code (originally in C)
 * available from the following site:
 * 
 *     http://hjemmesider.diku.dk/~torbenm/Planet
 * 
 * That page includes the following statement: "Both the program itself and 
 * maps created by the program are free for use, modification and reproduction,
 * both privately and for commercial purposes, as long as this does not limit
 * what other people may do with the program and the images they produce with
 * the program"
 *
 */
package rotp.model.planet;

import rotp.util.Base;

public final class PlanetHeightMap implements Base {
    static final float ROOT3 = (float)Math.sqrt(3.0);
    static final float dd1 = 0.45f;  	// weight for altitude difference 
    //static final float POWA = 1.0;    // power for altitude difference 
    static final float dd2 = 0.035f; 	// weight for distance 
    static final float POW = 0.47f;  	// power for distance function 

    float ssa,ssb,ssc,ssd, ssas,ssbs,sscs,ssds,
      ssax,ssay,ssaz, ssbx,ssby,ssbz, sscx,sscy,sscz, ssdx,ssdy,ssdz;

    private float seaPct = 0.50f;
    private byte seaLevel = 0;
    private int width = 800;
    private int height = 600; 
    private int depth;    			// depth of subdivisions 
    private final float r1;
    private final float r2;
    private final float r3; 	// seeds 
    private final float r4; 	// seeds
    private float longi, lat;
    private final float cla;
    private final float sla;
    private final float clo;
    private final float slo;
    private final byte[][] col;          // height array  -128 to 127
    private final int[] counts  = new int[256];
    private int totalCounts = 0;
    private long generateTime = -1;

    public void width(int w)       { width = w; }
    public void seaPct(float d)   { seaPct = bounds(0,d,1); }
    public int width()             { return width; }
    public int height()            { return height; }
    public byte[][] col()          { return col; }
    public byte[] col(int i)       { return col[i]; }
    public byte col(int x, int y)  { return col[x][y]; }
    public byte seaLevel()         { return seaLevel; }
    public long time()             { return generateTime; }

    public PlanetHeightMap(float seed, int radius, float sea) {
        seaPct = sea;
        longi = 0.0f;
        lat = 0.0f;

        // vars are read in at this point

        if (longi>180) 
                longi -= 360;
        longi = (float)Math.toRadians(longi);
        lat = (float)Math.toRadians(lat);

        sla = (float)Math.sin(lat); cla = (float)Math.cos(lat);
        slo = (float)Math.sin(longi); clo = (float)Math.cos(longi);

        height = radius * 2;
        width =  (int) Math.ceil(height * Math.PI);
        depth = 3*((int)(log_2(height)))+6;
        col = new byte[width][height];

        r1 = rand2(seed,seed);
        r2 = rand2(r1,r1);
        r3 = rand2(r1,r2);
        r4 = rand2(r2,r3);

        long start = System.currentTimeMillis();
        mollweide();
        seaLevel = calculateSeaLevel();
        generateTime = System.currentTimeMillis() - start;
    }
    public byte calculateSeaLevel() {
        int numNeeded = (int) (seaPct*totalCounts);
        int sum = 0;
        for (int i=0;i<counts.length;i++) {
            if (sum >= numNeeded)
                return (byte) (i+Byte.MIN_VALUE);
            sum += counts[i];
        }
        return Byte.MAX_VALUE;
    }
    public void mollweide() {
        float pi = (float) Math.PI;
        for (int j=0; j<height; j++) {
            float y1 = pi*(2.0f*j-height)/width;
            if (Math.abs(y1) > 1) {
                for (int i=0; i<width; i++) 
                    col[i][j] = Byte.MIN_VALUE;
            }
            else {
                float zz = (float)Math.sqrt(1-y1*y1);
                float y = 2/pi*(y1*zz+asin(y1));
                float cos2 = (float)Math.sqrt(1-y*y);
                if (cos2 > 0) {
                    float scale1 = (float)width/height/cos2/pi;
                    depth = 3*((int)(log_2(scale1*height)))+3;
                    boolean lastXMissing = false;
                    for (int i=0; i<width; i++) {
                        float theta1 =pi/zz*(2*i-width)/width;
                        if (Math.abs(theta1) > Math.PI) {
                            col[i][j] = Byte.MIN_VALUE;
                            lastXMissing = true;
                        } 
                        else {
                            theta1 += -0.5f*pi;
                            float x2 = (float)Math.cos(theta1)*cos2;
                            float y2 = y;
                            float z2 = -(float)Math.sin(theta1)*cos2;
                            float x3 = clo*x2+slo*sla*y2+slo*cla*z2;
                            float y3 = cla*y2-sla*z2;
                            float z3 = -slo*x2+clo*sla*y2+clo*cla*z2;
                            byte val = planet0(x3,y3,z3, i,j);
                            col[i][j] = val;
                            counts[val-Byte.MIN_VALUE]++;
                            totalCounts++;
                            if (lastXMissing) 
                                col[i-1][j] = val;
                            lastXMissing = false;
                        }
                    }
                }
            }
        }
    }
    private byte planet0(float x, float y, float z, int i, int j) {
        float alt = 4*planet1(x,y,z);

        int valInt = (int)(alt*256);
        byte valByte = (byte) valInt;
        if (valInt <= Byte.MIN_VALUE) 
            valByte = Byte.MIN_VALUE +1;
        else if (valInt >= Byte.MAX_VALUE) 
            valByte = Byte.MAX_VALUE;

        return valByte;
    }
    private float planet1(float x, float y, float z) {
        float abx = ssbx-ssax; float aby = ssby-ssay; float abz = ssbz-ssaz;
        float acx = sscx-ssax; float acy = sscy-ssay; float acz = sscz-ssaz;
        float adx = ssdx-ssax; float ady = ssdy-ssay; float adz = ssdz-ssaz;
        float apx = x-ssax; 	float apy = y-ssay; 	float apz = z-ssaz;

        if ((adx*aby*acz+ady*abz*acx+adz*abx*acy
                -adz*aby*acx-ady*abx*acz-adx*abz*acy)*
                (apx*aby*acz+apy*abz*acx+apz*abx*acy
                -apz*aby*acx-apy*abx*acz-apx*abz*acy) > 0)
        {
            // p is on same side of abc as d 
            if ((acx*aby*adz+acy*abz*adx+acz*abx*ady
                    -acz*aby*adx-acy*abx*adz-acx*abz*ady)*
                    (apx*aby*adz+apy*abz*adx+apz*abx*ady
                    -apz*aby*adx-apy*abx*adz-apx*abz*ady) > 0)
            {
                // p is on same side of abd as c 
                if ((abx*ady*acz+aby*adz*acx+abz*adx*acy
                        -abz*ady*acx-aby*adx*acz-abx*adz*acy)*
                        (apx*ady*acz+apy*adz*acx+apz*adx*acy
                        -apz*ady*acx-apy*adx*acz-apx*adz*acy) > 0)  
                {
                    // p is on same side of acd as b 
                    float bax = -abx;      float bay = -aby;      float baz = -abz;
                    float bcx = sscx-ssbx; float bcy = sscy-ssby; float bcz = sscz-ssbz;
                    float bdx = ssdx-ssbx; float bdy = ssdy-ssby; float bdz = ssdz-ssbz;
                    float bpx = x-ssbx;    float bpy = y-ssby;    float bpz = z-ssbz;
                    if ((bax*bcy*bdz+bay*bcz*bdx+baz*bcx*bdy
                         -baz*bcy*bdx-bay*bcx*bdz-bax*bcz*bdy)*
                        (bpx*bcy*bdz+bpy*bcz*bdx+bpz*bcx*bdy
                         -bpz*bcy*bdx-bpy*bcx*bdz-bpx*bcz*bdy) > 0)
                    {
                        // p is on same side of bcd as a 
                        // Hence, p is inside tetrahedron 
                        return planet(ssa,ssb,ssc,ssd, ssas,ssbs,sscs,ssds,
                                        ssax,ssay,ssaz, ssbx,ssby,ssbz,
                                        sscx,sscy,sscz, ssdx,ssdy,ssdz,
                                        x,y,z, 11);
                    }
                }
            }
        } 

        return planet(0,0,0,0,    		// initial altitude is 0 (midpoint) on all corners of tetrahedron 
                        r1,r2,r3,r4,     	// same seed set is used in every call 
                        -ROOT3-0.20f, -ROOT3-0.22f, -ROOT3-0.23f,  // coordinates of vertices of tetrahedron
                        -ROOT3-0.19f,  ROOT3+0.18f,  ROOT3+0.17f,
                        ROOT3+0.21f, -ROOT3-0.24f,  ROOT3+0.15f,
                        ROOT3+0.24f,  ROOT3+0.22f, -ROOT3-0.25f,
                        x,y,z,  			// coordinates of point we want colour of 
                        depth);  			// subdivision depth 
    }
    private float planet(float a, float b, float c, float d,	// altitudes of the 4 vertices
                        float as, float bs, float cs, float ds,	// seeds of the 4 vertices
                        float ax, float ay, float az,            // vertex coordinates
                        float bx, float by, float bz,   
                        float cx, float cy, float cz, 
                        float dx, float dy, float dz,
                        float x, float y, float z,		        // goal point
                        int level) 									// levels to go
    {
        if (level <= 0)
            return (a+b+c+d)/4;

        if (level == 11) {
            ssa=a; ssb=b; ssc=c; ssd=d; ssas=as; ssbs=bs; sscs=cs; ssds=ds;
            ssax=ax; ssay=ay; ssaz=az; ssbx=bx; ssby=by; ssbz=bz;
            sscx=cx; sscy=cy; sscz=cz; ssdx=dx; ssdy=dy; ssdz=dz;
        }

        float abx = ax-bx; float aby = ay-by; float abz = az-bz;
        float acx = ax-cx; float acy = ay-cy; float acz = az-cz;
        float lab = abx*abx+aby*aby+abz*abz;
        float lac = acx*acx+acy*acy+acz*acz;

        // reorder vertices so ab is longest edge 
        if (lab < lac)
            return planet(a,c,b,d, as,cs,bs,ds, ax,ay,az, cx,cy,cz, bx,by,bz, dx,dy,dz, x,y,z, level);

        float adx = ax-dx; float ady = ay-dy; float adz = az-dz;
        float lad = adx*adx+ady*ady+adz*adz;
        if (lab < lad)
            return planet(a,d,b,c, as,ds,bs,cs, ax,ay,az, dx,dy,dz, bx,by,bz, cx,cy,cz, x,y,z, level);

        float bcx = bx-cx; float bcy = by-cy; float bcz = bz-cz;
        float lbc = bcx*bcx+bcy*bcy+bcz*bcz;
        if (lab < lbc)
            return planet(b,c,a,d, bs,cs,as,ds, bx,by,bz, cx,cy,cz, ax,ay,az, dx,dy,dz, x,y,z, level);

        float bdx = bx-dx; float bdy = by-dy; float bdz = bz-dz;
        float lbd = bdx*bdx+bdy*bdy+bdz*bdz;
        if (lab < lbd)
            return planet(b,d,a,c, bs,ds,as,cs, bx,by,bz, dx,dy,dz, ax,ay,az, cx,cy,cz, x,y,z, level);

        float cdx = cx-dx; float cdy = cy-dy; float cdz = cz-dz;
        float lcd = cdx*cdx+cdy*cdy+cdz*cdz;
        if (lab < lcd)
            return planet(c,d,a,b, cs,ds,as,bs, cx,cy,cz, dx,dy,dz, ax,ay,az, bx,by,bz, x,y,z, level);

        // ab is longest, so cut ab 
        float es = rand2(as,bs);
        float es1 = rand2(es,es);
        float es2 = 0.5f+0.1f*rand2(es1,es1);
        float es3 = 1-es2;
        float ex, ey, ez;
        if (ax < bx) {
            ex = es2*ax+es3*bx; ey = es2*ay+es3*by; ez = es2*az+es3*bz;
        } else if (ax > bx) {
            ex = es3*ax+es2*bx; ey = es3*ay+es2*by; ez = es3*az+es2*bz;
        } else { // ax==bx, very unlikely to ever happen 
            ex = (ax+bx)/2; ey = (ay+by)/2; ez = (az+bz)/2;
        }
        if (lab > 1) 
            lab = (float)Math.sqrt(lab);
        // decrease contribution for very long distances 
        // new altitude is: 
        float e = (a+b)/2                                // average of end points 
               // + es*dd1*Math.pow(Math.abs(a-b),POWA) // commented out: POWA = 1.0, so simplify for speed
                    + es*dd1*(float)Math.abs(a-b)               // plus contribution for altitude diff 
                    + es1*dd2*(float)Math.pow(lab,POW);          // plus contribution for distance 
        float eax = ax-ex; float eay = ay-ey; float eaz = az-ez;
        float epx =  x-ex; float epy =  y-ey; float epz =  z-ez;
        float ecx = cx-ex; float ecy = cy-ey; float ecz = cz-ez;
        float edx = dx-ex; float edy = dy-ey; float edz = dz-ez;
        if ((eax*ecy*edz+eay*ecz*edx+eaz*ecx*edy
                -eaz*ecy*edx-eay*ecx*edz-eax*ecz*edy)*
                (epx*ecy*edz+epy*ecz*edx+epz*ecx*edy
                -epz*ecy*edx-epy*ecx*edz-epx*ecz*edy)>0.0)
                return planet(c,d,a,e, cs,ds,as,es, cx,cy,cz, dx,dy,dz, ax,ay,az, ex,ey,ez, x,y,z, level-1);
        else
                return planet(c,d,b,e, cs,ds,bs,es, cx,cy,cz, dx,dy,dz, bx,by,bz, ex,ey,ez, x,y,z, level-1);
    }
    /* random number generator taking two seeds */
    /* rand2(p,q) = rand2(q,p) is important     */
    private float rand2(float p, float q) {
        float pi = (float) Math.PI;
        float r = (p+pi)*(q+pi);
        return 2*(r-(int)r)-1;
    }
    private float log_2(float x) 	{ return (float) (Math.log(x)/Math.log(2)); }
}
