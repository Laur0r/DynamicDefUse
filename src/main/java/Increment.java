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

    public int[] sort(int[] a) {
        final int N = a.length;
        for (int i = 1; i < N; i++) { // N branches
            int j = i - 1;
            int x = a[i];
            // First branch (j >= 0):  2 + 3 + ... + N = N(N+1)/2 - 1 branches
            // Second branch (a[j] > x):  1 + 2 + ... + N-1 = (N-1)N/2 branches
            while ((j >= 0) && (a[j] > x)) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = x;
        }
        return a;
    }
}
