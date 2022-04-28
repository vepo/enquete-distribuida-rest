let evtSource = null;
window.addEventListener("beforeunload", function (event) {
    if (evtSource) {
        evtSource.close();
    }
});
moment.locale('pt-br');

angular.module('enqueteApp', ['ngMaterial', 'ngMessages', 'ng-fusioncharts'])
    .factory('BasicAuthInterceptor', function ($rootScope, $q) {
        return {
            request: function(config) {
                config.headers = config.headers || {};
                if ($rootScope.usuarioLogado) {
                    config.headers.Authorization = 'Basic ' + btoa($rootScope.usuarioLogado.nome + ":12345");
                }
                return config || $q.when(config);
            },
            response: function(response) {
                return response || $q.when(response);
            }
        };
    })
    .config(function ($mdDateLocaleProvider, $httpProvider) {
        $mdDateLocaleProvider.shortMonths = ['Jan', 'Fev', 'Mar', 'Abril', 'Maio', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
        $mdDateLocaleProvider.Months = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
        $mdDateLocaleProvider.days = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sabado'];
        $mdDateLocaleProvider.shortDays = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];
        // Example uses moment.js to parse and format dates.
        $mdDateLocaleProvider.parseDate = function (dateString) {
            var m = moment(dateString, 'L', true);
            return m.isValid() ? m.toDate() : new Date(NaN);
        };

        $mdDateLocaleProvider.formatDate = function (date) {
            var m = moment(date);
            return m.isValid() ? m.format('L') : '';
        };
        $httpProvider.interceptors.push('BasicAuthInterceptor');
    })
    .filter('dateFormatter', function () {
        return function (date, format) {
            if (!moment) {
                console.log('Error: momentJS is not loaded as a global');
                return '!momentJS';
            }
            moment.locale('pt-BR'); //needed if you want to change the locale globally
            if (!format) {
                return moment(date).fromNow();
            } else {
                return moment(date).format(format); //in absence of format parameter, return the relative time from the given date
            }
        }
    })
    .controller('AvisosController', function ($mdDialog, $log, $rootScope) {
        $rootScope.$on("Aviso", function (evnt, data) {
            $mdDialog.show($mdDialog.alert()
                .parent(angular.element(document.querySelector('#popupContainer')))
                .clickOutsideToClose(true)
                .title(data.titulo)
                .textContent(data.mensagem)
                .ariaLabel(data.escopo)
                .ok("OK")
                .targetEvent(evnt));
            //$mdToast.show($mdToast.simple()
            //                      .textContent(data.mensagem)
            //                      .position("top")
            //                      .hideDelay(3000))
            //        .then(function() {
            //          $log.log('Toast dismissed.');
            //        })
            //        .catch(function() {
            //          $log.log('Toast failed or was forced to close early by another toast.');
            //        });
        });
    })
    .controller('ListarEnquetesController', function ($http, $scope, $rootScope) {
        let controller = this;
        controller.enqueteEmVotacao = null;
        controller.resultados = null;
        controller.enquetes = [];
        controller.atualizar = () => {
            $http.get("/enquetes")
                .then(response => controller.enquetes = response.data,
                      response => $rootScope.$broadcast("Aviso", { escopo: "Enquetes", titulo: "Listar enquetes", mensagem: "Erro ao listar enquetes!" }));
        };
        controller.votos = [];
        controller.toggleSelection = (opcao) => {
            let idx = controller.votos.indexOf(opcao);
            // Is currently selected
            if (idx > -1) {
                controller.votos.splice(idx, 1);
            } else {
                controller.votos.push(opcao);
            }
        };
        controller.votar = (enquete) => {
            controller.enqueteEmVotacao = enquete;
            controller.votos = []
        };
        controller.votei = (enquete) => {
            return $rootScope.meusVotos.find(v => v.idEnquete == enquete.id);
        };
        controller.cancelarVotacao = () => {
            controller.enqueteEmVotacao = null;
            controller.votos = [];
            console.log('Cancelada!');
        };
        controller.submeterVotacao = () => {
            $http.put("/enquetes/" + controller.enqueteEmVotacao.id + "/votar", { opcoes: controller.votos, votanteId: $rootScope.usuarioLogado.id })
                .then(response => controller.cancelarVotacao(),
                      response => $rootScope.$broadcast("Aviso", { escopo: "Votação", titulo: "Registrar votação", mensagem: "Erro ao registrar votação!" }));
            console.log("Submitetida")
        };
        controller.verResultado = enquete => {
            controller.resultados = {
                titulo: enquete.titulo,
                dataSource: {
                    chart: {
                        caption: "Resultado",
                        subCaption: "Votos",
                        xAxisName: "Opção",
                        yAxisName: "Votos",
                        theme: "fusion",
                    },
                    data: enquete.opcoes.map(op => new Object({ label: op, value: 0 }))
                }
            };
            $http.get("/enquetes/" + enquete.id + "/resultados")
                 .then(response => controller.resultados.dataSource.data = response.data, 
                       response => $rootScope.$broadcast("Aviso", { escopo: "Votação", titulo: "Resultado votação", mensagem: "Não foi possível ver resultado da votação!" }));
        };
        controller.fecharResultados = () => controller.resultados = null;
        evtSource.onmessage = evnt => {
            let evento = JSON.parse(evnt.data);
            console.log("Novo evento", evento);
            if (evento.tipo == "NOVA_ENQUETE") {
                controller.enquetes.push(evento.dado);
                //console.log(controller.enquetes);
                //$rootScope.$broadcast("Aviso", { escopo: "Enquetes", titulo: "Nova enqueta", mensagem: "Nova enquete criada! Titulo: " + enquete.titulo });
            } else if (evento.tipo == "ENQUETE_FINALIZADA") {
                controller.enquetes.find(e => e.id == evento.dado.id).finalizada = true;
            }
            $scope.$apply();
        };
        controller.atualizar();
    })
    .controller('UsuariosController', function ($rootScope, $scope, $http) {
        let controller = this;
        let reload = () => {
            $scope.usuario = {
                nome: ""
            };
            controller.tab = 'cadastrar';
            controller.usuario = null;
            $rootScope.usuarioLogado = null;
            $rootScope.meusVotos = null;
            controller.usuariosDisponiveis = [];
            $http.get("/usuario")
                .then(response => controller.usuariosDisponiveis = response.data,
                    response => $rootScope.$broadcast("Aviso", { escopo: "Usuários", titulo: "Criar usuários", mensagem: "Erro ao listar usuários!" }));
        };

        controller.cadastrar = () => {
            console.log($scope.usuario);
            $http.post("/usuario", { nome: $scope.usuario.nome })
                .then(response => {
                    controller.usuario = response.data;
                    evtSource = new EventSource(`/enquetes/stream/${controller.usuario.id}`);
                    $rootScope.usuarioLogado = controller.usuario;
                    controller.lerVotos();
                    controller.usuariosDisponiveis.push(controller.usuario)
                },
                    response => $rootScope.$broadcast("Aviso", { escopo: "Usuários", titulo: "Criar usuários", mensagem: "Erro ao criar usuário!" }));
        };
        controller.acessar = () => {
            controller.usuario = controller.usuariosDisponiveis.find(u => u.nome == $scope.usuario.nome);
            evtSource = new EventSource(`/enquetes/stream/${controller.usuario.id}`);
            $rootScope.usuarioLogado = controller.usuario;
            controller.lerVotos();
        };
        controller.lerVotos = () => {
            $rootScope.meusVotos = null
            if ($rootScope.usuarioLogado) {
                $http.get("/enquetes/meus/votos")
                     .then(response => $rootScope.meusVotos = response.data,
                           response => console.log(response))
            }
        }
        controller.logout = () => {
            evtSource.close();
            evtSource = null;
            reload();
        };
        reload();
    })
    .controller('CadastroEnqueteController', function ($rootScope, $scope, $http) {
        let controller = this;
        let reload = () => {
            $scope.novaOpcao = moment();
            $scope.enquete = {
                titulo: "",
                idCriador: $rootScope.usuarioLogado.id,
                local: null,
                dataLimite: moment().add('months', 1),
                opcoes: []
            };
        };
        controller.salvar = () => {
            if ($scope.enquete.opcoes.length == 0 ||
                !$scope.enquete.titulo ||
                $scope.enquete.titulo.trim().length == 0 ||
                !$scope.enquete.local ||
                $scope.enquete.local.trim().length == 0) {
                $rootScope.$broadcast("Aviso", { escopo: "Enquetes", titulo: "Criar enquetes", mensagem: "Preencha todos os campos!" });
                return;
            }
            $scope.enquete.opcoes = $scope.enquete.opcoes.map(data => data.format('YYYY-MM-DD'));
            $scope.enquete.dataLimite = moment($scope.enquete.dataLimite).format('YYYY-MM-DD');
            $http.post("/enquetes", $scope.enquete)
                .then(response => $rootScope.$broadcast("Aviso", { escopo: "Enquetes", titulo: "Criar enquetes", mensagem: "Enquete criada!" }),
                    response => $rootScope.$broadcast("Aviso", { escopo: "Enquetes", titulo: "Criar enquetes", mensagem: "Erro ao criar enquete!" }));
            reload();
        };
        controller.adicionarOpcao = () => {
            $scope.enquete.opcoes.push($scope.novaOpcao);
            $scope.novaOpcao = moment($scope.novaOpcao).add('days', 1);
        };
        reload();
    });

