def generate_jenkinsfile():
    with open('Jenkinsfile', 'r', encoding='utf-8') as f:
        content = f.read()

    return content

with open('scratch/temp.py', 'w') as f:
    f.write('done')
