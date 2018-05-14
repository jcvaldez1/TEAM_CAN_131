import Jama.*;

public class MatrixTest{
	public static void main(String args[]){
		System.out.println("za warudo");
		double[][] vals = {
			{2, 0, 3},
			{1, 4, -1},
			{-2, -1, 0}
		};
		Matrix A = new Matrix(vals);
		A.print(6, 2);
		Matrix B = Matrix.identity(3, 3).times(2);
		B.print(6, 2);
	}
}