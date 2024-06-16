from langchain_community.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser
from langchain_community.document_loaders import WebBaseLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain.chains import RetrievalQA

#된장찌개
url = "https://en.wikipedia.org/wiki/Doenjang-jjigae"
url2 = "https://www.maangchi.com/recipe/doenjang-jjigae"
url3 = "https://www.koreanbapsang.com/doenjang-jjigae-korean-soy-bean-paste/"

#ganjang-gejang
#url = "https://www.koreanbapsang.com/ganjang-gejang-raw-crabs-marinated-in/"
#url2 = "https://www.maangchi.com/recipe/ganjang-gejang"
#url3 = "https://jeccachantilly.com/soy-marinated-raw-crab/"

#알밥
#url = "https://en.wikipedia.org/wiki/Albap"
#url2 = "http://www.lampcook.com/food/food_dic_global_view.php?idx_no=693"
#url3 = "https://www.maangchi.com/recipe/al-bap"

loader = WebBaseLoader(
    web_paths = (url, url2, url3)
)

#웹페이지 텍스트 -> Documents
docs = loader.load()

#print(len(docs))


text_splitter = RecursiveCharacterTextSplitter(
    chunk_size = 1000,
    chunk_overlap = 200
)
splited_docs = text_splitter.split_documents(docs)

# Indexing (Texts -> Embedding -> Store)
embeddings_model = HuggingFaceEmbeddings(
    model_name="sentence-transformers/all-MiniLM-L6-v2",
    model_kwargs={'device': 'cpu'},
    encode_kwargs={'normalize_embeddings': True},
)

vectorstore = Chroma.from_documents(documents=splited_docs, embedding=embeddings_model)

#LMM model
model = Ollama(model="llama3")

# Prompt
from langchain import hub

prompt = hub.pull("rlm/rag-prompt")

# Retrieval QA
qa_chain = RetrievalQA.from_chain_type(
    llm=model, 
    retriever=vectorstore.as_retriever(),
    chain_type_kwargs={"prompt": prompt}
)

food_name = "Doenjang-jjigae"
#"Doenjang-jjigae"

question  = "Let me know the information of {i}".format(i = food_name)
info = qa_chain.invoke({"query": question})
question2 = "Let me know the reipe of {i}".format(i=food_name)
recipe = qa_chain.invoke({"query": question2})

print(info)
print(recipe)