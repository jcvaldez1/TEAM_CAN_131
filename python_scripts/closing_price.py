import sys
import pandas as pd
import matplotlib.pyplot as plt
HIGH_PRICE = 0
MONTH_COLUMN = 7
DAY_COLUMN = 8

#read the data_set based off argument value
data = ['ap_data.csv', 'ceb_data.csv', 'chib_data.csv', 'gtcap_data.csv', 'mbt_data.csv', 'meg_data.csv', 'rlc_data.csv']

fig = plt.figure()
fig.suptitle('Closing Price against time', fontsize=20)
for i in range(len(data)):
	df = pd.read_csv('../csv_files/'+data[i])

	price = df.iloc[:,HIGH_PRICE].copy().tolist() 
	months = df.iloc[:,MONTH_COLUMN].copy().tolist()
	days = df.iloc[:,DAY_COLUMN].copy().tolist()

	previous_month = months[0]
	x_values = []
	additor = 30
	the_additor = 0
	for x in range(0,len(months)):
		if(months[x] != previous_month):
			previous_month = months[x]
			the_additor = the_additor + additor + (months[x-1] % 2)
			if(months[x-1] == 2):
				the_additor = the_additor - 2		
		x_values.append(the_additor + days[x])

	#fig = plt.figure()
	plt.plot(x_values,price, label = data[i].split('_')[0])
	#fig.suptitle('Closing Price against time', fontsize=20)
	plt.xlabel('Days', fontsize=18)
	plt.ylabel('Price', fontsize=16)
	plt.legend(loc = 'upper right')
fig.savefig('all' + '_closing_price')
plt.show()
