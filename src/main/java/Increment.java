public class Increment {

    public int x;
    public static int y;

    public Increment(){
        this.x = 5;
        y = 4;
    }

    public double add(Object z){
        if(x == 5){
            x = x+y;
            z.toString();
        } else {
            x = 5;
        }
        return x;
    }

    public int testWhile(long x, double z, String t){
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
}
