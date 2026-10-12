<div align="center">

# Pulse

**Um visualizador de áudio e vibração nos graves para Android, sem root**

[English](README.md) · [Português](README.pt.md)

</div>

---

### Introdução

O Pulse traz o visualizador de música *Pulse* das ROMs EvolutionX para qualquer Android, como um app independente. Ele desenha barras animadas sobre os seus apps enquanto a música toca e também pode transformar o grave da música em vibração, como um pequeno bass shaker.

Ele escuta a **saída global de áudio** pela API `Visualizer` do Android, então funciona com qualquer app de música ou vídeo. Roda como um Serviço de Acessibilidade: sem notificação fixa e com custo quase zero quando nada está tocando.

> [!NOTE]
> O Pulse é um projeto independente, inspirado no recurso Pulse do EvolutionX. Não tem ligação com o EvolutionX.

---

### Recursos

**Visualizador**

*   Quatro estilos: barras, barras com degradê, blocos que esmaecem e onda.
*   Cores: tema do sistema (Monet), cor personalizada em hex, arco-íris e lava lamp animada com velocidade ajustável.
*   Quantidade de barras, altura, espaço, arredondamento, sensibilidade e opacidade ajustáveis.
*   Modo espelhado (graves no centro), picos que caem devagar e barras no topo ou na base da tela.
*   Predefinições prontas: Clássico, Neon, Minimalista, Blocos retrô e Onda.
*   Exibição opcional na tela de bloqueio e opção de esconder as barras na horizontal (útil para vídeos).
*   Limite da taxa de captura para economizar bateria.

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
*   Diagnóstico copiável para relatar problemas.
*   Idiomas: Português e English, selecionáveis dentro do app.

---

### Compatibilidade

Desenvolvido e testado no **Android 13**. O código usa só o framework do Android (sem bibliotecas externas), com verificações para versões mais novas. Recomendado Android 8.0 (API 26) ou superior.

> [!TIP]
> Root **não é necessário**. Ele só é usado por um atalho opcional que concede permissões e ativa o serviço para você.

---

### Instalação

1. Compile o projeto (por exemplo, com o AndroidIDE ou o Gradle) e instale o APK.
2. Abra o Pulse e permita o acesso ao áudio.
3. Ligue o serviço do Pulse nas configurações de Acessibilidade do Android.
4. Toque uma música.

Se o Android bloquear o serviço como "configuração restrita", abra as informações do app, use o menu (⋮) e escolha **Permitir configurações restritas**.

**Opcional (root):** o atalho de root concede o áudio, libera as configurações restritas, isenta o app das restrições de bateria e ativa o serviço em um só passo.

---

### Compilação

*   Projeto Java com o pacote `com.pulse.pulse`. Os fontes ficam em `app/src/main`.
*   Sem dependências: não usa AppCompat nem Material, então não precisa do tema do modelo do projeto.
*   Os textos ficam em `res/values` (inglês) e `res/values-pt` (português).

---

### Como funciona

1. Um `Visualizer` global captura a saída de áudio e devolve uma FFT.
2. As barras mapeiam faixas da FFT (escala logarítmica) para alturas, com suavização de attack/decay.
3. Na vibração, a energia da faixa de frequência escolhida é normalizada, comparada com uma linha de base lenta (para separar batidas de grave sustentado), passa por threshold e curva, é suavizada por um envelope de attack/release, limitada e enviada ao motor de vibração.

Os detalhes do algoritmo e da integração estão em [`HAPTICS.md`](HAPTICS.md).

---

### Limitações

*   Cada faixa da FFT cobre cerca de 47 Hz (1024 amostras a 48 kHz), então subgrave e grave são separados por pesos, sem precisão fina.
*   A taxa de captura costuma ser de cerca de 20 Hz.
*   As barras não podem ser desenhadas na tela ociosa (always-on / ambient), porque apps não conseguem desenhar ali. A vibração pode funcionar.
*   A vibração em segundo plano depende da versão do Android e da ROM. Se o feedback tátil do sistema estiver desligado, a vibração pode ser ignorada.
*   O áudio processado por DSPs (como ViPER4Android ou JamesDSP) costuma ser capturado já processado, mas isso depende do aparelho. Use o medidor ao vivo para conferir.

---

### Privacidade

*   Sem permissão de internet.
*   O áudio é analisado no aparelho e nunca é guardado nem enviado.
*   O serviço lê apenas o **nome do pacote do app na tela**, para aplicar o filtro de apps. Ele não lê o conteúdo da tela.

---

### Solução de problemas

*   Barras paradas: confira se a permissão de áudio foi concedida e se o serviço está ligado. O estado mostra o erro, se houver.
*   Sem vibração: use **Testar vibração** e confira as configurações de vibração e feedback tátil do sistema.
*   Para com a tela apagada: desative a otimização de bateria do Pulse.
*   Pedindo ajuda: copie o diagnóstico na tela Sobre e envie.
*   Logs: `su -c "logcat -d | grep -i pulse"`

---

### Histórico de mudanças

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

---

### Licença

Adicione sua licença aqui.
