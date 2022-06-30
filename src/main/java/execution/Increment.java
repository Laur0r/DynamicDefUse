package execution;

public class Increment {

    public int y;

    public Increment(){
        y=4;
    }

    public int gcd(int x, int y){
        while (y != x){
            if(x > y){
                x = x -y;
            } else {
                y = y - x;
            }
        }
        return x;
    }

    public int testWhile(long x, double z){
        int y = 0;
        while(x != 5){
            y = 5;
            if(x == 2){
                x = 4;
                y = 3;
            } else {
                x = y;
            }
        }
        return y;
    }

    public int testArray(int[] a){
        int i = a[0];
        a[1] = 5;
        return a[0];
    }

    public int test(int x) {
        int z = gcd(x, y);
        return z;
    }
}
