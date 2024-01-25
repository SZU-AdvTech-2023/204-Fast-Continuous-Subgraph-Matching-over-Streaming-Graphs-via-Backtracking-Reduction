import csv

csv_file_path = 'lastfm10.csv'
# =SUM(A2:A101)
# =SUM(B2:B101)
# =SUM(C2:C101)
# =SUM(A2:A51)
# =SUM(B2:B51)
# =SUM(C2:C51)
# =SUM(A52:A101)
# =SUM(B52:B101)
# =SUM(C52:C101)
# 指定文件路径
file_path = './lastfm10.txt'

data = []
# 使用 'utf-8' 编码打开文件，以确保正确处理中文字符
with open(file_path, 'r', encoding='utf-8') as file:
    # 逐行读取文件
    for line in file:
        # print(line)


        if line.startswith("Q"):
            row_data = []
            # print(line)
            split_parts = line.split()
            print(split_parts[4])
            ss = split_parts[5].split(":")
            print(ss[1])
            row_data.append(split_parts[4])
            row_data.append(ss[1])
        if line.startswith("U"):
            ss = line.split(":")
            print(ss[1].strip())
            row_data.append(ss[1].strip())
            data.append(row_data)
        # print(line.strip())  # .strip() 用于删除行尾的换行符和空白字符


# 写入 CSV 文件
with open(csv_file_path, 'w', newline='', encoding='utf-8') as csv_file:
    csv_writer = csv.writer(csv_file)
    # 写入表头
    csv_writer.writerow(["Updated Matches", "Backtrackings", "Updating Totally Cost (ms)"])
    # 写入数据
    csv_writer.writerows(data)
# print("aa".startswith("a"))