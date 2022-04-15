const evtSource = new EventSource("/enquetes/stream");
moment.locale('pt-br');
angular.module('enqueteApp', ['ngMaterial', 'ngMessages'])
    .config(function ($mdDateLocaleProvider) {
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
    })
    .controller('ListarEnquetesController', function ($http, $scope) {
        let controller = this;
        controller.enquetes = [];
        controller.atualizar = () => {
            $http.get("/enquetes")
                .then(response => controller.enquetes = response.data, response => alert(response.statusText));
        };
        evtSource.onmessage = enquete => {
            controller.enquetes.push(JSON.parse(enquete.data));
            console.log(controller.enquetes);
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
            controller.usuariosDisponiveis = [];
            $http.get("/usuario")
                .then(response => controller.usuariosDisponiveis = response.data, response => alert(response.statusText));
        };

        controller.cadastrar = () => {
            console.log($scope.usuario);
            $http.post("/usuario", { nome: $scope.usuario.nome })
                .then(response => {
                    controller.usuario = response.data;
                    $rootScope.usuarioLogado = controller.usuario;
                    controller.usuariosDisponiveis.push(controller.usuario)
                }, response => alert(response.statusText));
        };
        controller.acessar = () => {
            controller.usuario = controller.usuariosDisponiveis.find(u => u.nome == $scope.usuario.nome);
            $rootScope.usuarioLogado = controller.usuario;
        };
        controller.logout = reload;
        reload();
    })
    .controller('CadastroEnqueteController', function ($rootScope, $scope, $http) {
        let controller = this;
        let reload = () => {
            $scope.novaOpcao = moment();
            $scope.enquete = {
                titulo: "",
                criador: $rootScope.usuarioLogado.nome,
                local: null,
                opcoes: []
            };
        };
        controller.salvar = () => {
            $scope.enquete.opcoes = $scope.enquete.opcoes.map(data => data.format('YYYY-MM-DD'));
            $http.post("/enquetes", $scope.enquete)
                .then(response => console.log(response), response => console.log(response));
            reload();
        };
        controller.adicionarOpcao = () => {
            $scope.enquete.opcoes.push($scope.novaOpcao);
            $scope.novaOpcao = moment($scope.novaOpcao).add('days', 1);
        };
        reload();
    });

