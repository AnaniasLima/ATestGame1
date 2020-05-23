#include <Servo.h>

#define CMD_NOTEIRO     1
#define CMD_STATUS_RQ   2
#define CMD_DEMO        3
#define CMD_PLAY        4
#define CMD_LED         5

#define ACTION_RESET    1
#define ACTION_ON       2
#define ACTION_OFF      3
#define ACTION_QUESTION 4
#define SIMULA5REAIS    5
#define SIMULA10REAIS   6
#define SIMULA20REAIS   7
#define SIMULA50REAIS   8


#define DELAY_ANTES_PKT     delay(20); // wait for 20ms
#define DELAY_DEPOIS_PKT   delay(50); // wait for 50ms

int numPktResp=1;

int led = 0;
int flagValorNumerico=0;

char *strRet="";
char data = 0;            //Variable for storing received data
int ind=0;

char strToken[30];
char strValor[30];
//char strTimestamp[30];
//char strNoteiroOnTimestamp[30];
char strHora[30];

char strCmd[30];
char strAction[30];

int estado=0;
int flagTokenValor=0;
int packetNumber=0;
int pacotesNaoReconhecidos=0;
int iComando;
int iAction;

volatile int statusNoteiro=0;
int valorNoteiro=0;
int erro=0;
int statusPlay=0;
int statusDemo=0;
int fsmStateDemo=0;

#define AGUARDANDO_START       0
#define AGUARDANDO_ABRE_ASPAS  1
#define AGUARDANDO_TOKEN       2
#define AGUARDANDO_DOIS_PONTOS 3
#define AGUARDANDO_VALOR       4
#define AGUARDANDO_VIRGULA     5

void zeraNovoPacote(void);

char bufResposta[300];
int indBufResposta;

//
// ---------------------------------------------
void startResposta(const char *cmd) {
  indBufResposta = 0;
  
  bufResposta[indBufResposta++] = '{';
  bufResposta[indBufResposta++] = '"';
  bufResposta[indBufResposta++] = 'c';
  bufResposta[indBufResposta++] = 'm';
  bufResposta[indBufResposta++] = 'd';
  bufResposta[indBufResposta++] = '"';
  bufResposta[indBufResposta++] = ':';
  bufResposta[indBufResposta++] = '"';
  while (*cmd) {
     bufResposta[indBufResposta++] = *cmd++;
  }
  bufResposta[indBufResposta++] = '"';
}
  
//
// ---------------------------------------------
void addStrResposta(const char *cmd, const char *param) {

  bufResposta[indBufResposta++] = ',';
  bufResposta[indBufResposta++] = '"';
  while (*cmd) {
     bufResposta[indBufResposta++] = *cmd++;
  }
  bufResposta[indBufResposta++] = '"';
  bufResposta[indBufResposta++] = ':';
  bufResposta[indBufResposta++] = '"';
  while (*param) {
     bufResposta[indBufResposta++] = *param++;
  }
  bufResposta[indBufResposta++] = '"';
}

//
// ---------------------------------------------
void addIntResposta(const char *cmd, int valor) {
  char strValor[10];
  char *pStr = strValor;

   
  sprintf(strValor, "%d", valor);
  bufResposta[indBufResposta++] = ',';
  bufResposta[indBufResposta++] = '"';
  while (*cmd) {
     bufResposta[indBufResposta++] = *cmd++;
  }
  bufResposta[indBufResposta++] = '"';
  bufResposta[indBufResposta++] = ':';
  while (*pStr) {
     bufResposta[indBufResposta++] = *pStr++;
  }
}

//
// ---------------------------------------------
void sendResposta(void) {


  bufResposta[indBufResposta++] = '}';
  bufResposta[indBufResposta] = '\0';

//  DELAY_ANTES_PKT;
  
  Serial.println(bufResposta); 
  
//  DELAY_DEPOIS_PKT;

}


void setup()
{
  zeraTudo();
  Serial.begin(115200);   //Sets the baud for serial data transmission                               
//  Serial.begin(57600);   //Sets the baud for serial data transmission                               
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.print("Estou vivo");          //Print Value inside data in Serial monitor
}


void processaPar(char *token, char *valor) {
  if ( strcmp(token, "cmd") == 0 ) {
    if ( strcmp(valor, "fw_noteiro") == 0 ) {
        iComando = CMD_NOTEIRO;
    } else if ( strcmp(valor, "fw_status_rq") == 0 ) {
        iComando = CMD_STATUS_RQ;
    } else if ( strcmp(valor, "fw_demo") == 0 ) {
        iComando = CMD_DEMO;
    } else if ( strcmp(valor, "fw_play") == 0 ) {
        iComando = CMD_PLAY;
    } else if ( strcmp(valor, "fw_led") == 0 ) {
        iComando = CMD_LED;
    } else {
        iComando = 0;
    }
  } else if ( strcmp(token, "action") == 0 ) {
    if ( strcmp(valor, "reset") == 0 ) {
      iAction = ACTION_RESET;
    } else if ( strcmp(valor, "on") == 0 ) {
      iAction = ACTION_ON;
    } else if ( strcmp(valor, "off") == 0 ) {
      iAction = ACTION_OFF;
    } else if ( strcmp(valor, "question") == 0 ) {
      iAction = ACTION_QUESTION;
    } else if ( strcmp(valor, "simula5") == 0 ) {
      iAction = SIMULA5REAIS;
    } else if ( strcmp(valor, "simula10") == 0 ) {
      iAction = SIMULA10REAIS;
    } else if ( strcmp(valor, "simula20") == 0 ) {
      iAction = SIMULA20REAIS;
    } else if ( strcmp(valor, "simula50") == 0 ) {
      iAction = SIMULA50REAIS;
    } else {
      iAction = 0;
    }
  } else if ( strcmp(token, "packetNumber") == 0 ) {
    packetNumber = atoi(valor);
    if (packetNumber == 1 ) {
//      numPktResp=1; // Para resincronizar as respostas
    }

  } else if ( strcmp(token, "hour") == 0 ) {
    strcpy(strHora, valor);
  }
}

void txFsmStateDemo () {
  char *strResp = "FSM_IDLE";

  addIntResposta("fsmStateDemo", fsmStateDemo);

  if ( fsmStateDemo == 0 ) {
      statusDemo = 0; 
  } else {
      if ( statusDemo == 999) {
        statusDemo=1;
        strResp = "RUNNING_DEMO";
        fsmStateDemo = 5 + random(1, 40);
      } else {
        if ( fsmStateDemo <= 4 ) {
              switch(fsmStateDemo ) {
                case 4 : strResp = "RUNNING_DEMO_WAIT_Y"; break;
                case 3 : strResp = "RUNNING_DEMO_WAIT_XZ"; break;
                case 2 : strResp = "HOMMING_START_Y"; break;
                case 1 : strResp = "HOMMING_PARALLEL_WAIT_END"; break;
              }
        } else {
           strResp = "RUNNING_DEMO";
        }
        fsmStateDemo--;
      }
  }
  
  addStrResposta("fsm_state", strResp);
}

void txFsmStatePlay () {
     addStrResposta("fsm_state", "FSM_IDLE");
}


void processaLinha(void) 
{   const char *action="????";
    const char *status="zzz";
    const char *resp="ok";
    int respostaCurta=0;
    
  switch ( iComando ) {
    case CMD_NOTEIRO :
      switch ( iAction ) {
        case ACTION_RESET : 
              valorNoteiro=0;
              action = "reset";
              break;
        case ACTION_ON    :
              statusNoteiro=1;
              action = "on";
              break;
        case ACTION_OFF   : 
              statusNoteiro=0;
              action = "off";
              break;
        case ACTION_QUESTION: 
              action = "question";
              break;

        case SIMULA5REAIS: 
              respostaCurta=1;
              if ( statusNoteiro ) {
                valorNoteiro+=5;
                statusNoteiro=0;
              } else {
                erro = 1;
              }
              action = "simula5";
              break;

        case SIMULA10REAIS: 
              respostaCurta=1;
              if ( statusNoteiro ) {
                valorNoteiro+=10;
                statusNoteiro=0;
              } else {
                erro = 1;
              }
              action = "simula10";
              break;

        case SIMULA20REAIS: 
              respostaCurta=1;
              if ( statusNoteiro ) {
                valorNoteiro+=20;
                statusNoteiro=0;
              } else {
                erro = 1;
              }
              action = "simula20";
              break;

        case SIMULA50REAIS: 
              respostaCurta=1;
              if ( statusNoteiro ) {
                valorNoteiro+=50;
                statusNoteiro=0;
              } else {
                erro = 1;
              }
              action = "simula50";
              break;
      }
      
      if (statusNoteiro == 0 ) {
        status="off";
      } else {
        status="on";
      }

      startResposta("fw_noteiro");
      
      if (  respostaCurta ) {
          addStrResposta("action", action);
          addIntResposta("packetNumber", packetNumber);
          addIntResposta("numPktResp", numPktResp++);
          addStrResposta("ret", (erro == 0) ? "ok" : "error");
          sendResposta();
      } else {
          addStrResposta("action", action);
          if (iAction == ACTION_QUESTION ) {
            addStrResposta("status", status);
            addIntResposta("statusNoteiro", statusNoteiro);
          }
          addIntResposta("value", valorNoteiro);
          addIntResposta("packetNumber", packetNumber);
          addIntResposta("numPktResp", numPktResp++);
          addStrResposta("hour", strHora);
          addStrResposta("ret", (erro == 0) ? "ok" : "error");
          
//        addStrResposta("fsm_state", "FSM_IDLE");
          sendResposta();
      }
      break;  

    case CMD_STATUS_RQ :
      switch ( iAction ) {
        case ACTION_QUESTION: action = "question"; break;
      }

      startResposta("fw_status_rq");
      addStrResposta("action", action);
      addIntResposta("error_n", 0);
      addStrResposta("button_1", "off");
      addStrResposta("button_2", "off");
      addIntResposta("mifare", 0);
      addIntResposta("mifare_pass", 0);
      addIntResposta("packetNumber", packetNumber);
      addIntResposta("numPktResp", numPktResp++);
      addStrResposta("hour", strHora);
      addStrResposta("ret", (erro == 0) ? "ok" : "error");
      txFsmStateDemo();
      sendResposta();
      break;  

    case CMD_DEMO :

      strRet = "ok";

      switch ( iAction ) {
        case ACTION_ON : 
            action = "on"; 
            if ( (statusDemo == 0) && (statusPlay == 0) ) {
              statusDemo=999; 
              fsmStateDemo = 999;
            } else {
              strRet = "busy";
            }
            break;
        case ACTION_OFF : 
          statusDemo=0; 
          action = "off"; 
          fsmStateDemo = 1;
          break;
      }

      startResposta("fw_demo");
      addStrResposta("action", action);
      addIntResposta("error_n", 0);
      addIntResposta("packetNumber", packetNumber);
      addIntResposta("numPktResp", numPktResp++);
      addStrResposta("hour", strHora);
      addStrResposta("ret", (erro == 0) ? strRet : "error");
      txFsmStateDemo();
      sendResposta();
      break;  

    case CMD_PLAY :

      switch ( iAction ) {
        case ACTION_ON        : statusPlay=1; action = "on"; break;
        case ACTION_OFF       : statusPlay=0; action = "off"; break;
        case ACTION_QUESTION  : action = "question"; break;
      }
      DELAY_ANTES_PKT;
      Serial.println("");  
      Serial.println("{");  
      Serial.println("\"cmd\": \"fw_play\",");  
      Serial.print("\"action\": ");   Serial.print("\""); Serial.print(action);  Serial.println("\",");
      if ( iAction == ACTION_QUESTION ) {
        static int conta;
        static int premio=0;
        int ganhou=0;
        resp = "busy";
        if ( (++conta % 6) == 0 )  {
            resp="ok";
            premio++;
            if ( (premio % 2) == 0 )  {
              ganhou = 1;          
            }
        }
        Serial.print("\"premio_n\": ");   Serial.print(ganhou, DEC);  Serial.println(",");
      }
//      Serial.print("\"timestamp\": ");  Serial.print("\""); Serial.print(strTimestamp);  Serial.println("\",");
//      Serial.print("\"noteiroOnTimestamp\": ");  Serial.print("\""); Serial.print(strNoteiroOnTimestamp);  Serial.println("\",");
      Serial.print("\"packetNumber\": ");   Serial.print(packetNumber, DEC);  Serial.println(",");
      Serial.print("\"numPktResp\": ");   Serial.print(numPktResp++, DEC);  Serial.println(",");
      Serial.print("\"hour\": ");   Serial.print("\""); Serial.print(strHora);  Serial.println("\",");
      Serial.print("\"ret\": ");   Serial.print("\""); Serial.print(resp);  Serial.println("\"");
      txFsmStatePlay();
      Serial.println("}"); 
      break;  

    case CMD_LED :
      switch ( iAction ) {
        case ACTION_ON: 
              digitalWrite(LED_BUILTIN, HIGH); 
              action = "on";
              break;
        case ACTION_OFF: 
              digitalWrite(LED_BUILTIN, LOW); 
              action = "off";
              break;
      }
      DELAY_ANTES_PKT;
      Serial.println("");  
      Serial.println("{");  
      Serial.println("\"cmd\": \"fw_led\",");  
      Serial.print("\"action\": ");   Serial.print("\""); Serial.print(action);  Serial.println("\",");
      Serial.print("\"packetNumber\": ");   Serial.print(packetNumber, DEC);  Serial.println(",");
      Serial.print("\"numPktResp\": ");   Serial.print(numPktResp++, DEC); 
      Serial.println("}"); 
      break;  


    default: 
      DELAY_ANTES_PKT;
      pacotesNaoReconhecidos++;
      Serial.println("");  
      Serial.println("{");  
      Serial.println("\"cmd\": \"fw_nack\",");  
      Serial.print("\"packetNumber\": ");   Serial.print(packetNumber, DEC);  Serial.println(",");
      Serial.print("\"  \": ");   Serial.print(numPktResp++, DEC);  Serial.println(",");
      Serial.print("\"qtdNacks\": ");   Serial.print(pacotesNaoReconhecidos, DEC);  
      Serial.println("}"); 
      break;
  }
}


void loop()
{

  if ( Serial.available() > 0) {
    data = Serial.read();        //Read the incoming data & store into data
    if ( data == '\n') {
//      if ( led ) {
//           digitalWrite(LED_BUILTIN, HIGH); 
//           led = 0;
//      } else {
//           digitalWrite(LED_BUILTIN, LOW); 
//           led = 1;
//      }
      return;
    }

    if ( data == '}') {

      if (packetNumber == 1 ) {
        zeraControleEstados();
      }

      processaLinha( );
      
      if (packetNumber == 1 ) {
        zeraTudo();
      }
      estado = AGUARDANDO_START;
      flagTokenValor=0;
      return;
    }

    switch(estado) {
      case AGUARDANDO_START : 
          if ( data == '{') {
            zeraNovoPacote();
            estado = AGUARDANDO_ABRE_ASPAS;
          }
          break;
      case AGUARDANDO_ABRE_ASPAS : 
          if ( data == '"') {
            flagValorNumerico=0;
            if ( flagTokenValor == 0 ) {
              estado = AGUARDANDO_TOKEN;
            } else {
              estado = AGUARDANDO_VALOR;
            }
            ind=0;
            strValor[ind] = '\0';
          } else {
            if ( flagTokenValor == 1){
              if ( (data >= '0') && (data <= '9')) {
                flagValorNumerico=1;
                estado = AGUARDANDO_VALOR;
                ind=0;
                strValor[ind++] = data;
                strValor[ind] = '\0';
              }
            }
          }
          break;
      case AGUARDANDO_TOKEN : 
          if ( data == '"') {
            estado = AGUARDANDO_DOIS_PONTOS;
          } else {
            strToken[ind++] = data;
            strToken[ind] = '\0';
          }
          break;
      case AGUARDANDO_DOIS_PONTOS : 
          if ( data == ':') {
            estado = AGUARDANDO_ABRE_ASPAS;
            flagTokenValor=1;
          }
          break;
      case AGUARDANDO_VALOR : 
          if ( data == '"') {
            estado = AGUARDANDO_VIRGULA;
            processaPar(strToken, strValor);
            flagTokenValor=0;
          } else {
            if (flagValorNumerico  ) {
                if ( data == ',') {
                    processaPar(strToken, strValor);
                    flagTokenValor=0;
                    estado = AGUARDANDO_ABRE_ASPAS;
                    break;            
                }
            }
            strValor[ind++] = data;
            strValor[ind] = '\0';
          }
          break;
      case AGUARDANDO_VIRGULA : 
          if ( data == ',') {
            estado = AGUARDANDO_ABRE_ASPAS;
            flagTokenValor=0;
          }
          break;
    }      
  }
}

void zeraControleEstados()
{
  statusNoteiro=0;
  valorNoteiro=0;
  statusPlay=0;
  statusDemo=0;
  fsmStateDemo = 0;
}


void zeraTudo(void)
{
  estado=0;
  pacotesNaoReconhecidos=0;

  zeraControleEstados();
  zeraNovoPacote();
}

void zeraNovoPacote(void)
{
  ind=0;
  led = 0;
  erro=0;
  flagValorNumerico=0;
  strToken[0] = '\0';
  strValor[0] = '\0';
  strHora[0] = '\0';

  strCmd[0] = '\0';
  strAction[0] = '\0';

  flagTokenValor=0;
  packetNumber=0;
  iComando=0;
  iAction=0;
}
