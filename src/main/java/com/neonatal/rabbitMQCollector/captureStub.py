import csv
import time

def create_csv(freq):
    # Define the column headings for the CSV file
    headings = ['Time', 'ECG_HR', 'NIBP_Systolic', 'NIBP_Diastolic', 'NIBP_Mean', 'SpO2', 'ET_CO2', 'AA_ET', 'AA_FI', 'AA_MAC_SUM', 'Agent_AA', 'O2_FI', 'N2O_FI', 'N2O_ET', 'CO2_RR', 'T1_Temp', 'T2_Temp', 'P1_HR', 'P1_Systolic', 'P1_Diastolic', 'P1_Mean', 'P2_HR', 'P2_Systolic', 'P2_Diastolic', 'P2_Mean', 'P3_HR', 'P3_Systolic', 'P3_Diastolic', 'P3_Mean', 'PPeak', 'PPlat', 'TV_Exp', 'TV_Insp', 'PEEP', 'MV_Exp', 'Compliance', 'RR', 'NMT_MODE', 'NMT_TWITCH_RATIO', 'NMT_T1', 'ST_II', 'ST_V5', 'ST_aVL', 'EEG_Entropy', 'EMG_Entropy', 'BSR_Entropy', 'BIS', 'BIS_BSR', 'BIS_EMG', 'BIS_SQI', 'NMT_Count', 'NMT_T1', 'NMT_T2', 'NMT_T3', 'NMT_T4', 'SPV', 'PPV', 'MAC_AGE_SUM']

    
    # Define an example row of data for the CSV file
    example_data = ['17/03/2023 13:11', 120, '-', '-', '-', 97, '-', '-', '-', '-', 'None', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-','-']

    
    #Open the CSV file in write mode and write the column headings and example row to it
    with open('S5DataExport.csv', mode='w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(headings)
        print("t")
        while True:
            print("34")
            writer.writerow(example_data)
            file.flush()
            time.sleep(freq)
            

if __name__ == '__main__':
    create_csv(5)
