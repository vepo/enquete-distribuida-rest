angular.module('enqueteApp', [])
    .controller('EnquetesController', function ($http) {
        let controller = this;
        controller.enquetes = [];
        controller.atualizar = () => {
            $http.get("/enquetes")
                .then(response => controller.enquetes = response.data, response => alert(response.statusText));
        };
        controller.atualizar();
    })
    .controller('CadastroController', function ($scope, $http) {
        let controller = this;

        let reload = () => {
            $scope.usuario = {
                nome: ""
            };
            controller.tab = 'cadastrar';
            controller.usuario = null;
            controller.usuariosDisponiveis = [];
            $http.get("/usuario")
                .then(response => controller.usuariosDisponiveis = response.data, response => alert(response.statusText));
        };

        controller.cadastrar = () => {
            console.log($scope.usuario);
            $http.post("/usuario", { nome: $scope.usuario.nome })
                .then(response => controller.usuario = response.data, response => alert(response.statusText));
        };
        controller.acessar = () => controller.usuario = controller.usuariosDisponiveis.find(u => u.nome == $scope.usuario.nome);
        ;
        controller.logout = reload;
        reload();
    });

