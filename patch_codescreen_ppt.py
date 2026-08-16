import re

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'r') as f:
    content = f.read()

# Add ppt handling
content = content.replace(
    '                                name.endsWith(".pdf") -> PdfViewer(fileNode.file)',
    '''                                name.endsWith(".pdf") -> PdfViewer(fileNode.file)
                                name.endsWith(".ppt") || name.endsWith(".pptx") -> PptViewer(fileNode.file)'''
)

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'w') as f:
    f.write(content)

