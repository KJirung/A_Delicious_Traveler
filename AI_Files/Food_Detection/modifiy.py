import os

def modify_dataset_in_folder(input_folder, output_folder):
    # 입력 폴더 내의 모든 파일 목록 가져오기
    files = os.listdir(input_folder)
    
    # 입력 폴더 내의 각 파일에 대해 수정 작업 수행
    for file_name in files:
        input_file_path = os.path.join(input_folder, file_name)
        output_file_path = os.path.join(output_folder, file_name)
        modify_dataset(input_file_path, output_file_path)
        
def modify_dataset(input_file, output_file):
    with open(input_file, 'r') as f:
        lines = f.readlines()

    modified_lines = []
    for line in lines:
        parts = line.strip().split(' ')
        if len(parts) < 5:
            continue
        parts[0] = '0'  # 클래스 레이블을 0으로 변경
        modified_line = ' '.join(parts) + '\n'
        modified_lines.append(modified_line)

    with open(output_file, 'w') as f:
        f.writelines(modified_lines)

if __name__ == "__main__":
    input_folder = "datasets/test/labels"  # 입력 폴더 경로
    output_folder = "datasets/test/modified_labels"  # 수정된 파일을 저장할 폴더 경로
    
    # 출력 폴더가 없으면 생성
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
    
    modify_dataset_in_folder(input_folder, output_folder)
    print("Dataset modification completed.")
