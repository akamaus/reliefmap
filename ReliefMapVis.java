import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.security.*;
import javax.swing.*;
import javax.imageio.*;

public class ReliefMapVis {
    int W, H;		//x- and y-sizes of the map
    double[][] hs;	//the actual elevation map
    String[] contour;	//the contour map
    int nmes;		//number of measurements done
    int maxmes;		//max number of measurements possible
    boolean ok;		//all measurements were valid
    int[] dx = {1,1, 1,0, 0,-1,-1,-1};
    int[] dy = {1,0,-1,1,-1, 1, 0,-1};
    char[][] cont;
    double[] ret;
    SecureRandom r;
// exter
    int[] extr_x, extr_y;
    // -----------------------------------------
    static int join(int r, int g, int b){
        return (r<<16)|(g<<8)|b;
    }
    // -----------------------------------------
    static int color(double d){
        double r, g, b;
        if(d < 1./6){
            b = d*6; r = 0; g = 0;
        }else if(d<2./6){
            b=1; r = 0; g = (d-1./6)*6;
        }else if(d < 1.0/2){
            b = 1-(d-2./6)*6; r = 0; g = 1;
        }else if(d < 4./6){
            b = 0; r = (d-3./6)*6; g = 1;
        }else if(d < 5./6){
            b = 0; r = 1; g = 1-(d-4./6)*6;
        }else{
            r = 1;
            b = (d-5./6)*6;
            g = (d-5./6)*6;
        }
        return join((int)(255.99*r),(int)(255.99*g),(int)(255.99*b));
    }
    // -----------------------------------------
    void generate(String seed) {
      try {
        r = SecureRandom.getInstance("SHA1PRNG");
        r.setSeed(Long.parseLong(seed));
        H = r.nextInt(451)+50;
        W = r.nextInt(451)+50;
System.out.println("W = "+W+"\nH = "+H);
        hs = new double[H][W];
        //generate the elevation map
        int Nextr = r.nextInt(11)+15;
        int[] x, y, z, f;
        int i, j, k, l;
        double[] w;
        double n, d, dist;
        extr_x = x = new int[Nextr];
        extr_y = y = new int[Nextr];
        z = new int[Nextr];
        f = new int[Nextr];
        w = new double[Nextr];

        cont = new char[H][W];
        for (i=0; i<H; i++)
        for (j=0; j<W; j++)
            cont[i][j]='0';

        for (k=0; k<Nextr; k++)
        {   x[k] = r.nextInt(W);
            y[k] = r.nextInt(H);
            z[k] = r.nextInt(100);
            f[k] = r.nextInt(100)+100;
            w[k] = r.nextDouble()*0.5+0.3;
            cont[y[k]][x[k]] = '2';
        }
        for (i=0; i<H; i++)
        for (j=0; j<W; j++)
        {   n = 0;
            d = 0;
            for (k=0; k<Nextr; k++)
            {   //distance between point (j,i) and extremum k
                dist = Math.sqrt(Math.pow(i-y[k],2) + Math.pow(j-x[k],2));
                dist = Math.pow(dist+f[k], -w[k]);
                n += dist*z[k];
                d += dist;
            }
            hs[i][j] = n/d;
        }
        double hmin=hs[0][0], hmax=hs[0][0];
        for (i=0; i<H; i++)
        for (j=0; j<W; j++)
        {   hmin = Math.min(hmin, hs[i][j]);
            hmax = Math.max(hmax, hs[i][j]);
        }
        //scale to 0..100 heights
        for (i=0; i<H; i++)
        for (j=0; j<W; j++)
            hs[i][j] = (hs[i][j]-hmin)/(hmax-hmin)*100;

        //convert it to the contour map
        double D;
        D = r.nextDouble()*8.0+2.0;
System.out.println("D = "+D);
for(d = 0; d<100; d+=D){//contour
    for(j = 0; j<H; j++){
        for(k = 0; k<W; k++){
            if(hs[j][k]<d){
                for(l = 0; l<8; l++){
                    int jj = j+dx[l];
                    int kk = k+dy[l];
                    if(jj>=0 && jj<H && kk >=0 &&kk<W && hs[jj][kk] >d){
                        cont[jj][kk] = '1';
                    }
                }
            }
        }
    }
}

        //add dots in places of local _strict_ extremums
        boolean ismin, ismax;
        for (i=1; i<H-1; i++)
        for (j=1; j<W-1; j++)
        {   ismin=true;
            ismax=true;
            for (k=0; k<8; k++)
            {   if (hs[i][j] <= hs[i+dx[k]][j+dy[k]])
                    ismax=false;
                if (hs[i][j] >= hs[i+dx[k]][j+dy[k]])
                    ismin=false;
            }
            if (ismin || ismax)
            {   cont[i][j]='1';
                cont[i-1][j]='1';
                cont[i+1][j]='1';
                cont[i][j-1]='1';
                cont[i][j+1]='1';
            }
        }

        //convert to param
        contour = new String[H];
        for (i=0; i<H; i++)
            contour[i] = new String(cont[i]);
      }
      catch (Exception e) { 
        System.err.println("An exception occured while generating the test case.");
        e.printStackTrace(); 
      }
    }
    // --------- library function --------------
    public double measure(int x, int y) {
        if (x<0 || x>=W)
        {   addFatalError("Invalid x-coordinate of the measurement "+x+".");
            ok = false;
            return -1;
        }
        if (y<0 || y>=H)
        {   addFatalError("Invalid y-coordinate of the measurement "+y+".");
            ok = false;
            return -1;
        }
	if (nmes == maxmes)
	{   addFatalError("Number of measurements exceeded the maximal number of measurements avaliable.");
            ok = false;
	    return -1;
	} 
        nmes++;
        return Math.max(Math.min(hs[y][x] + r.nextGaussian(),100.0),0.0);	//values 0..100
    }
    // -----------------------------------------
    public double runTest(String seed) {
      try {
        generate(seed);

        //pass the params and get the result
        nmes = 0;
        maxmes = (int)Math.sqrt(H*W);
        ok = true;
        ret = getMap(contour);
        if (!ok)
            return 0.0;

        //estimate the result
        double avgsqerr = 0.0;
        int i,j;
        for (i=0; i<H; i++)
        for (j=0; j<W; j++)
            avgsqerr += Math.pow(hs[i][j]-ret[i*W+j], 2);
        avgsqerr/=(W*H);
System.out.println("Average squared error = "+avgsqerr);
System.out.println("Number of measurements = "+nmes+" of "+maxmes+" possible");
        return 1 / (avgsqerr * Math.pow(100, nmes*1.0/maxmes));
      }
      catch (Exception e) { 
        System.err.println("An exception occurred while trying to get your program's results.");
        e.printStackTrace(); 
        return 0.0;
      }
    }
// ------------- visualization part ------------
    static String exec;
    static String file;
    static String diff;
    static double diffsc;
    static Process proc;
    InputStream is;
    OutputStream os;
    // -----------------------------------------
    public double[] getMap(String[] cont) throws IOException {
        int i;
        //imitate passing params to getMap
        StringBuffer sb = new StringBuffer();
        sb.append(cont.length).append('\n');
        for (i=0; i<cont.length; i++)
            sb.append(cont[i]).append('\n');
        os.write(sb.toString().getBytes());
        os.flush();
        //imitate solution's calls of measure
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String t;
        int x,y;
        double h;
        while ((t=br.readLine()).equals("?"))
        {   //get params of next measurement
            x = Integer.parseInt(br.readLine());
            y = Integer.parseInt(br.readLine());
            h = measure(x,y);
            if (!ok)
                return new double[0];
            //and return them
            os.write((h+"\n").getBytes());
            os.flush();
        }
        //imitate receiving return from getMap
        //W*H elements, in rowwise order
        double[] ret = new double[H*W];
        for (i=0; i<W*H; i++)
            ret[i] = Double.parseDouble(br.readLine());
        return ret;
    }
    // -----------------------------------------
    BufferedImage drawMap(double[][] map, int mode) {
        //draws a map in colors 0..cmax
        BufferedImage bi = new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
        int i,j,c;
        double hmin=map[0][0], hmax=map[0][0];
        if (mode==0)
        for (i=0; i<H; i++)
        for (j=0; j<W; j++)
        {   hmin = Math.min(hmin, map[i][j]);
            hmax = Math.max(hmax, map[i][j]);
        }
        for (i=0; i<H; i++)
            for (j=0; j<W; j++) {   
                if (mode==0){	//main mode - for original elevation map
                    if (contour[i].charAt(j)=='1')	//black contour lines
                        c = 0;
                    else if (contour[i].charAt(j)=='2')
                        c = 0xFFFFFF;
                    else
                        c = color((map[i][j]-hmin)/(hmax-hmin));
                }else{		//secondary mode - for error coding
                    if (map[i][j]>=0)  //red
                        c = (int)(255.99*(Math.min(map[i][j]/diffsc,1)))*0x10000;
                    else		   //blue
                        c = (int)(255.99*(Math.min(-map[i][j]/diffsc,1)))*0x1;
                    //System.out.println(map[i][j]+" "+diffsc);
                }
                bi.setRGB(j,i,c);
            }
            return bi;
    }
    // -----------------------------------------
    public ReliefMapVis(String seed) throws IOException {
        //interface for runTest
        if (exec != null) {
            try {
                Runtime rt = Runtime.getRuntime();
                proc = rt.exec(exec);
                os = proc.getOutputStream();
                is = proc.getInputStream();
                new ErrorReader(proc.getErrorStream()).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Score = "+runTest(seed));

        int shift = 0;
        //visualize, if required
        if (file != null)
        {   //draw the actual elevation map
            final BufferedImage bi = drawMap(hs,0);
            if (file.equals("-")) 
            {   JFrame jf = new JFrame();
                jf.setSize(W+8,H+30);
                shift = H+30;
                jf.getContentPane().add(new JPanel(){
                    public void paint(Graphics g){
                        g.drawImage(bi,0,0,null);
                    }
                });
                jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                jf.setVisible(true);
            }
            else
                ImageIO.write(bi,"png",new File(file+".png"));
        }
        if (diff != null)
        {   //draw the errors map
            double[][] err = new double[H][W];
            for (int i=0; i<H; i++)
            for (int j=0; j<W; j++)
                err[i][j] = ret[i*W+j]-hs[i][j];
            final BufferedImage bi = drawMap(err,1);
            if (diff.equals("-")) 
            {   JFrame jf = new JFrame();
                jf.setLocation(0,shift);
                jf.setSize(W+8,H+30);
                jf.getContentPane().add(new JPanel(){
                    public void paint(Graphics g){
                        g.drawImage(bi,0,0,null);
                    }
                });
                jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                jf.setVisible(true);
            }
            else
                ImageIO.write(bi,"png",new File(diff+".png"));
        }

    }
    // -----------------------------------------
    public static void main(String[] args) throws IOException {
        String seed = "1";
        diffsc = 10.;
        for (int i = 0; i<args.length; i++)
        {   if (args[i].equals("-seed"))
                seed = args[++i];
            if (args[i].equals("-exec"))
                exec = args[++i];
            if (args[i].equals("-vis"))
                file = args[++i];
            if (args[i].equals("-diff"))
                diff = args[++i];
            if (args[i].equals("-diffscale"))
                diffsc = Double.parseDouble(args[++i]);
        }
        ReliefMapVis f = new ReliefMapVis(seed);
    }
    // -----------------------------------------
    void addFatalError(String message) {
        System.out.println(message);
    }
}

class ErrorReader extends Thread{
    InputStream error;
    public ErrorReader(InputStream is) {
        error = is;
    }
    public void run() {
        try {
            byte[] ch = new byte[50000];
            int read;
            while ((read = error.read(ch)) > 0)
            {   String s = new String(ch,0,read);
                System.out.print(s);
                System.out.flush();
            }
        } catch(Exception e) { }
    }
}
