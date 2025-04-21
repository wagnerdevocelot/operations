FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Instalar dependências necessárias (como curl, rlwrap e git)
RUN apt-get update && apt-get install -y curl rlwrap git && rm -rf /var/lib/apt/lists/*

# Instalar Clojure CLI
RUN curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh \
    && chmod +x linux-install.sh \
    && ./linux-install.sh \
    && rm linux-install.sh

# Copiar arquivos do projeto
COPY . /app/

# Instalar dependências e criar o uberjar
# Usar clojure diretamente pois está no PATH após a instalação
RUN clojure -T:build ci

# Exposição da porta de serviço
EXPOSE 3000

# Comando para executar a aplicação
CMD ["java", "-jar", "target/operations-0.1.0-SNAPSHOT.jar"]