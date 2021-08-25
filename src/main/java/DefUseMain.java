public class DefUseMain {

	public static void main(String[] args) {
		long startTime = System.nanoTime();
		Increment inc = new Increment();
		System.out.println(inc.y);
		int[] a = new int[]{1, 3,2};
		a = inc.sort(a);
		System.out.println(a[0]+" "+a[1]+" "+a[2]);
		long endTime = System.nanoTime();
		long totalTime = endTime - startTime;
		System.out.println("Execution Time: "+totalTime);
	}

}
