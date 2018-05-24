/*To run this program:
	javac CsvToMatrixParser.java
	java CsvToMatrixParser <csv filename>.csv
*/
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;
import Jama.*;


public class CsvToMatrixParser{
	public static void main(String[] args){
		CsvToMatrixParser me = new CsvToMatrixParser(); //creates an instance of itself to access its non static methods
		ArrayList<String[]> dataRows = null; //initialize arrayList of mutable rows to hold parsed data
		Matrix dataMatrix = null;
		try{
			dataRows = me.parseCsvToList(args[0]); //calls parseCsvToList method with args[0] as fileName
		}catch(ArrayIndexOutOfBoundsException e){
			e.printStackTrace();//catch if no extra args were given during program exec
			System.exit(1);
		}

		int colIs[] = {6, 7, 8, 0};//indexes for [y, m, d, c] columns
		dataMatrix = me.parseListToMatrix(dataRows, colIs);//calls method to convert dataRows list to a matrix with only colIs as columns
		dataMatrix.print(6,2); //prints matrix with column width of 6 and 2 numbers after decimal point
	}

	//returns a a parsed Arraylist of rows from the csv of fileName
	public ArrayList<String[]> parseCsvToList(String fileName){
		File file = new File(fileName);//File object for Scanner to read
		ArrayList<String[]> dataRows = new ArrayList<String[]>();//what the function will return
		try{
			Scanner scanFile = new Scanner(file);//Scanner object for reading file
			String row = scanFile.nextLine(); //skip the column headers
			while(scanFile.hasNextLine()){
				row = scanFile.nextLine();	//get row
				dataRows.add(row.split(",")); //split row into columns
			}
			scanFile.close();
		}catch(FileNotFoundException e){
			System.out.println("I dont find no file of that name sonny boi");
			System.exit(1);
		}
		return dataRows;
	}

	//returns a parsed Matrix from the dataRows list and and int array of column indexes
	public Matrix parseListToMatrix(ArrayList<String[]> dataRows, int[] colIs){
		Matrix dataMatrix = new Matrix(dataRows.size(), colIs.length);//what will be returned
		for(int i = 0; i < dataRows.size(); i++){//traverse rows
			for(int j = 0; j < colIs.length; j++){//traverse columns
				String dataString = dataRows.get(i)[colIs[j]];//extract string data at dataRow i, and column index specified by colIs
				try{
					double dataNum = Double.parseDouble(dataString);//parse data string into double
					dataMatrix.set(i, j, dataNum);//set value of dataMatrix[i][j]
				}catch(NumberFormatException e){
					e.printStackTrace();//catch if dataString unparsable to double
					System.exit(1);
				}
			}
		}
		return dataMatrix;
	}

}
