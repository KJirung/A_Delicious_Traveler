from langchain_community.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.prompts import PromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.pydantic_v1 import BaseModel, Field
from langchain_community.document_loaders import WebBaseLoader
from langchain_text_splitters import CharacterTextSplitter
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_openai import OpenAIEmbeddings
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.retrievers.multi_query import MultiQueryRetriever
from langchain.schema.runnable import RunnablePassthrough
import chromadb
import os
import bs4

os.environ["OPENAI_API_KEY"] = "sk-proj-9bqCxnT2uuZHgegb2ZbPT3BlbkFJsxs7lwW553eZBsQpDmjK"

from langchain_community.llms import Ollama

llm = Ollama(model="llama3")

# 자료구조 정의 (pydantic)
class CusineRecipe(BaseModel):
    name: str = Field(description="name of a cusine")
    info: str = Field(description="information of a cusine")
    recipe: str = Field(description="recipe to cook the cusine")

# 출력 파서 정의
output_parser = JsonOutputParser(pydantic_object=CusineRecipe)

format_instructions = output_parser.get_format_instructions()

# prompt 구성
prompt = PromptTemplate(
    template="Answer the user query.\n{format_instructions}\n{query}\n",
    input_variables=["query"],
    partial_variables={"format_instructions": format_instructions},
)

chain = prompt | llm | output_parser

# Query를 사용하여 체인을 실행
query = "Let me know the information of Miyeok-guk and How to cook Miyeok-guk"
response = chain.invoke(query)

print(response)