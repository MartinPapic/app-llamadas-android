import glob
import sys
try:
    import PyPDF2
    with open(glob.glob("Levantamiento*.pdf")[0], 'rb') as f:
        reader = PyPDF2.PdfReader(f)
        with open("pdf_output.txt", "w", encoding="utf-8") as out:
            for page in reader.pages:
                out.write(page.extract_text() + "\n")
except Exception as e:
    print(str(e))
