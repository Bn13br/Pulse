<div align="center">

# Pulse

**Um visualizador de áudio e vibração nos graves para Android, sem root**

[English](README.md) · [Português](README.pt.md)

</div>

---

### Introdução

O Pulse traz o visualizador de música *Pulse* das ROMs EvolutionX para qualquer Android, como um app independente. Ele desenha barras animadas sobre os seus apps enquanto a música toca, pode colori-las com a **capa do álbum da música que está tocando** e também pode transformar o grave da música em vibração, como um pequeno bass shaker.

Ele escuta a **saída global de áudio** pela API `Visualizer` do Android, então funciona com qualquer app de música ou vídeo. Roda como um Serviço de Acessibilidade: sem notificação fixa e com custo quase zero quando nada está tocando.

> [!NOTE]
> O Pulse é um projeto independente, inspirado no recurso Pulse do EvolutionX. Não tem ligação com o EvolutionX.

---

### Recursos

**Visualizador**

*   Quatro estilos: barras, barras com degradê, blocos que esmaecem e onda.
*   Cores: tema do sistema (Monet), cor personalizada em hex, arco-íris, lava lamp animada com velocidade ajustável e **cores da capa do álbum**.
*   Quantidade de barras, altura, espaço, arredondamento, sensibilidade e opacidade ajustáveis.
*   Modo espelhado (graves no centro), picos que caem devagar e barras no topo ou na base da tela.
*   Predefinições prontas: Clássico, Neon, Minimalista, Blocos retrô, Onda e Capa do álbum.
*   Exibição opcional na tela de bloqueio e opção de esconder as barras na horizontal (útil para vídeos).
*   Limite da taxa de captura para economizar bateria.

**Cores da capa do álbum**

*   As barras ganham as cores da capa da mídia que está tocando, em qualquer app de música que compartilhe a capa com o Android.
*   Escolha entre usar só a cor dominante ou misturar até três cores principais da capa ao longo das barras.
*   As cores mudam suavemente quando a música muda, sem pular de uma vez.
*   Funciona com barras, degradê, blocos e onda, inclusive no modo espelhado.
*   Capas em preto e branco dão barras em cinza claro, e capas escuras ganham um tom mais vivo, para as barras continuarem visíveis.
*   Volta para a cor do tema do sistema enquanto não houver capa para ler.
*   Nada roda para esse recurso, a menos que o modo esteja selecionado e o Pulse esteja ligado.

**Vibração nos graves**

*   Converte a energia dos graves (não o volume geral) em intensidade da vibração, usando análise espectral.
*   Modos: Subgrave, Grave, Batida e Personalizado (sua própria faixa de frequência).
*   Controles: sensibilidade, intensidade máxima, threshold, bass boost (só na análise), attack, release, constância, normalização automática e limitador.
*   Predefinições: Suave, Batida marcante, Bass shaker e Subgrave profundo.
*   Vibração opcional com a tela apagada ou ociosa.
*   Medidor de graves ao vivo e botão de teste, para conferir se funciona e como o seu DSP interfere.
*   Funciona em aparelhos sem controle de amplitude (usa pulsos de duração diferente).

**Controle**

*   Tile nos atalhos rápidos para ligar e desligar o Pulse.
*   Filtro de apps: esconder ou mostrar só nos apps escolhidos.
*   Detecção de estado: o Pulse avisa se está ativo, pausado, sem alguma permissão ou sem conseguir ler o áudio.
*   Exportar e importar seus ajustes como texto, e restaurar a aparência ou tudo.
*   Diagnóstico copiável para relatar problemas, incluindo o estado das cores da capa.
*   Segue o idioma do sistema: Português e English.

---

### Compatibilidade

Desenvolvido e testado no **Android 13**. O código usa só o framework do Android (sem bibliotecas externas), com verificações para versões mais novas. Recomendado Android 8.0 (API 26) ou superior.

Compila com **Gradle 7.4.2** e **Android Gradle Plugin 7.3.1** (a combinação usada pelo AndroidIDE), com `compileSdk` 33, `minSdk` 26 e Java 8.

> [!TIP]
> Root **não é necessário**. Ele só é usado por atalhos opcionais que concedem permissões e ativam o serviço para você.

---

### Instalação

1. Compile o projeto (por exemplo, com o AndroidIDE ou o Gradle) e instale o APK.
2. Abra o Pulse e permita o acesso ao áudio.
3. Ligue o serviço do Pulse nas configurações de Acessibilidade do Android.
4. Toque uma música.

Se o Android bloquear o serviço como "configuração restrita", abra as informações do app, use o menu (⋮) e escolha **Permitir configurações restritas**.

**Cores da capa do álbum (opcional):** escolha *Cores da capa do álbum* nas opções de cor e toque em **Conceder acesso**. O Android abre a lista de acesso às notificações, onde você liga o Pulse. Essa permissão é o que permite ao Android compartilhar a mídia atual com o app. A mesma regra de "configuração restrita" pode valer aqui.

**Opcional (root):** o atalho de root concede o áudio, libera as configurações restritas, isenta o app das restrições de bateria e ativa o serviço em um só passo. As cores da capa têm um botão próprio, **Conceder com root**, que também libera o acesso às notificações.

---

### Compilação

*   Projeto Java com o pacote `com.pulse.pulse`. Os fontes ficam em `app/src/main`.
*   Testado com Gradle 7.4.2 e Android Gradle Plugin 7.3.1. Um plugin mais novo (8.x) exige Gradle 8.0 ou superior.
*   Sem dependências: não usa AppCompat, Material, Kotlin nem a biblioteca Palette, então o bloco `dependencies` do modelo do projeto pode ser removido. A extração das cores da capa é própria.
*   O app usa o próprio `PulseTheme` (`res/values/pulse_theme.xml`). Se o modelo do projeto criou arquivos `themes.xml` apontando para o Material (em `res/values` e `res/values-night`), apague-os ou deixe-os vazios.
*   Os textos ficam em `res/values` (inglês) e `res/values-pt` (português). O idioma do app segue o idioma do sistema, e o inglês é usado para qualquer outro idioma.
*   Se você está atualizando da 2.0, apague o antigo `Lang.java`, que não é mais usado.

`app/build.gradle.kts` mínimo:

```kotlin
plugins {
    id("com.android.application")
}

android {
    namespace = "com.pulse.pulse"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.pulse.pulse"
        minSdk = 26
        targetSdk = 33
        versionCode = 21
        versionName = "2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
```

---

### Como funciona

1. Um `Visualizer` global captura a saída de áudio e devolve uma FFT.
2. As barras mapeiam faixas da FFT (escala logarítmica) para alturas, com suavização de attack/decay.
3. Na vibração, a energia da faixa de frequência escolhida é normalizada, comparada com uma linha de base lenta (para separar batidas de grave sustentado), passa por threshold e curva, é suavizada por um envelope de attack/release, limitada e enviada ao motor de vibração.
4. Nas cores da capa, um listener de notificações lê a capa da sessão de mídia ativa: primeiro as imagens dos metadados da mídia, depois links de imagem locais e, por último, o ícone grande da notificação de mídia. A capa é reduzida, os pixels são agrupados por matiz e os grupos mais vivos viram a paleta. As barras então fazem a transição para a nova paleta.

Os detalhes do algoritmo da vibração e da integração estão em [`HAPTICS.md`](HAPTICS.md).

---

### Limitações

*   Cada faixa da FFT cobre cerca de 47 Hz (1024 amostras a 48 kHz), então subgrave e grave são separados por pesos, sem precisão fina.
*   A taxa de captura costuma ser de cerca de 20 Hz.
*   As barras não podem ser desenhadas na tela ociosa (always-on / ambient), porque apps não conseguem desenhar ali. A vibração pode funcionar.
*   A vibração em segundo plano depende da versão do Android e da ROM. Se o feedback tátil do sistema estiver desligado, a vibração pode ser ignorada.
*   O áudio processado por DSPs (como ViPER4Android ou JamesDSP) costuma ser capturado já processado, mas isso depende do aparelho. Use o medidor ao vivo para conferir.
*   As cores da capa dependem de o app de música compartilhar a capa com o Android. Apps que não compartilham não são suportados, e o Pulse mantém a cor do tema para eles.
*   Capas que existem só como link da internet (http/https) não podem ser lidas, porque o Pulse não tem permissão de internet.
*   O Android exige o acesso às notificações para esse modo. Ele não pode ser trocado por outra permissão.

---

### Privacidade

*   Sem permissão de internet.
*   O áudio é analisado no aparelho e nunca é guardado nem enviado.
*   O serviço lê apenas o **nome do pacote do app na tela**, para aplicar o filtro de apps. Ele não lê o conteúdo da tela.
*   As cores da capa são opcionais. Elas usam o acesso às notificações do Android só para ler a **capa da mídia que está tocando**. O Pulse procura apenas a notificação de mídia, nunca lê o título nem o texto de nenhuma notificação, e nada é guardado nem enviado. O listener fica parado, a menos que o modo esteja selecionado e o Pulse esteja ligado.

---

### Solução de problemas

*   Barras paradas: confira se a permissão de áudio foi concedida e se o serviço está ligado. O estado mostra o erro, se houver.
*   Sem vibração: use **Testar vibração** e confira as configurações de vibração e feedback tátil do sistema.
*   Para com a tela apagada: desative a otimização de bateria do Pulse.
*   Cores da capa ficam na cor do tema: confira se o acesso às notificações está ligado para o Pulse, se a música está mesmo tocando e se o app de música mostra a capa no player. Pause e toque de novo, ou troque a música, para atualizar.
*   Não consigo ligar o acesso às notificações: ele pode estar bloqueado como configuração restrita. Abra as informações do app, use o menu (⋮) e escolha **Permitir configurações restritas**, ou use o botão **Conceder com root**.
*   Cores desbotadas ou cinza: provavelmente a própria capa é em preto e branco ou muito clara. Desligue *Usar várias cores da capa* para usar só a cor dominante.
*   Erro de build "Minimum supported Gradle version is 8.0": o projeto usa um plugin mais novo que o seu Gradle. Use o plugin 7.3.1 com o Gradle 7.4.2, ou mude o wrapper para o Gradle 8.0 ou superior.
*   Erro de build "resource style/Theme.Material3.DayNight not found": apague os arquivos `themes.xml` deixados pelo modelo do projeto, ou deixe-os vazios.
*   Erro de build "cannot find symbol ... Prefs.LANG": um `Lang.java` antigo da 2.0 ainda está no projeto. Apague-o.
*   Pedindo ajuda: copie o diagnóstico na tela Sobre e envie.
*   Logs: `su -c "logcat -d | grep -i pulse"`

---

### Histórico de mudanças

**2.1**

*   **Cores da capa do álbum:** novo modo de aparência em que as barras ganham as cores da capa da mídia que está tocando, com opção de usar uma ou várias cores e transição suave entre as músicas.
*   **Predefinição Capa do álbum:** ajuste pronto do visualizador para o novo modo.
*   **Acesso opcional:** o modo usa o acesso às notificações do Android, com atalho para a tela certa das configurações e um botão opcional de root.
*   **Idioma:** o app agora segue o idioma do sistema. O seletor de idioma dentro do app foi removido.
*   **Diagnóstico:** agora inclui o idioma do sistema e o estado das cores da capa.
*   **Build:** sem dependências, testado com Gradle 7.4.2 e Android Gradle Plugin 7.3.1.

**2.0**

*   **Estilos novos:** blocos que esmaecem e onda, além do arredondamento.
*   **Lava lamp:** cor animada com velocidade ajustável.
*   **Predefinições:** ajustes prontos para o visualizador e para a vibração nos graves.
*   **Esconder na horizontal:** opção para esconder as barras quando a tela é girada.
*   **Backup:** exportar e importar seus ajustes como texto, e restaurar tudo.
*   **Detecção de estado:** o app agora avisa quando o serviço está desligado, falta uma permissão ou não dá para ler o áudio.
*   **Diagnóstico:** resumo do aparelho e dos ajustes para copiar e relatar problemas.
*   **Bateria:** atalho para as configurações de otimização de bateria.
*   Interface reorganizada, com temas claro e escuro.

**1.x**

*   Visualizador com barras, degradê, modo espelhado, picos e cores Monet/personalizada/arco-íris.
*   Vibração nos graves com os modos Subgrave, Grave, Batida e Personalizado, attack/release, limitador, constância e botão de teste.
*   Vibração com a tela apagada ou ociosa.
*   Tile nos atalhos rápidos, filtro de apps, opção da tela de bloqueio e limite da taxa de captura.
*   Atalho opcional de root.
*   Português e English.

---

### Créditos

*   [EvolutionX](https://evolution-x.org/): inspiração para o recurso Pulse.
*   API `Visualizer` do Android: captura de áudio e FFT.
*   API `MediaSession` do Android: acesso à capa da mídia que está tocando.

---

### Licença

Adicione sua licença aqui.
